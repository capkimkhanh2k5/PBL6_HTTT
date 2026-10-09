# Florence-2-base-ft + Quyet Small: local DANASEA experiment

> **Đã ngừng sử dụng trong phương án triển khai (08/10/2026).** Theo phạm vi đã chốt, backend chỉ tích hợp Quyet cho quyết định trên text/JSON. Thư mục này giữ kết quả thử nghiệm cũ để đối chiếu; không được import hoặc khởi động bởi pipeline DANASEA. Xem [triển khai hiện tại](../../docs/AI-IMPLEMENTATION.md).

Đã tải đúng checkpoint **`microsoft/Florence-2-base-ft`**, kiểm tra SHA-256 của weights với LFS được công bố và chạy suy luận offline trên Apple M4 Pro, RAM 24 GB. Florence nạp được trên CPU và MPS; Quyet Small nhận JSON từ Florence trong một tiến trình riêng.

**Kết luận hiện tại:** có thể dùng Florence để trích xuất mô tả, đối tượng và chữ; regex cho thông tin liên hệ là phần hứa hẹn nhất trong bộ thử nhỏ. Cấu hình Quyet Small được thử ở đây chưa đáng tin cậy để tự động phân loại, loại bỏ ảnh hoặc duyệt bài. Chưa có bằng chứng bộ đôi này tương đương một vision-language model tổng quát.

## Model và môi trường đã cài

Microsoft phát hành repository model và weights theo [MIT](https://huggingface.co/microsoft/Florence-2-base-ft/blob/f6c1a25888ffc1d945ee8a1a77ac833c7303d46e/LICENSE). Giữ thông báo bản quyền/giấy phép khi phân phối; các thành phần phụ thuộc có giấy phép riêng. [Model card chính thức](https://huggingface.co/microsoft/Florence-2-base-ft) mô tả các tác vụ caption, detection, OCR và grounding.

| Thành phần | Bản đã dùng |
| --- | --- |
| Florence | `microsoft/Florence-2-base-ft`, revision `f6c1a25888ffc1d945ee8a1a77ac833c7303d46e` |
| Số tham số thực tế | 231,414,016 |
| Florence weights | 463,221,266 bytes, khoảng 442 MiB; chỉ tải safetensors, không tải thêm `.bin` |
| Florence runtime | Python 3.11.16, Torch 2.14.1, Transformers 4.49.0, float32, eager attention |
| Quyet | `chinhnc/Quyet-1.0-Small`, revision `233167bba5df61b5375a522bf8a042d8d2189379` |
| Quyet runtime | Môi trường đã cài ở experiment `quyet-small`, package `quyet` 1.0.2, Transformers 5.19.0 |

Florence dùng môi trường riêng vì checkpoint gốc Microsoft có custom code theo API Transformers cũ. Không hạ phiên bản Transformers của Quyet. Hai bên trao đổi qua JSON; không thay weights Microsoft bằng bản chuyển đổi của cộng đồng.

Weights Florence ở:

```text
/Users/capkimkhanh/.cache/danasea/florence-base-ft/models/f6c1a25888ffc1d945ee8a1a77ac833c7303d46e
```

Python Florence ở `/Users/capkimkhanh/.cache/danasea/florence-base-ft/.venv/bin/python`. Weights và venv nằm ngoài Git. `download.json` lưu hashes; runner kiểm tra lại tất cả file model trước khi nạp custom code. Inference dùng `local_files_only=True` và các biến offline. Việc chuẩn bị/download ban đầu có sử dụng mạng.

## Phạm vi và cách đọc kết quả

Tám ảnh đầu vào: ba ảnh thật công khai (SUP, kayak, ô tô) và năm ảnh chữ được tạo bằng Pillow (quảng cáo Việt/Anh, cảnh báo chống trả tiền riêng, thông tin SUP bình thường, chỉ dẫn cố thay đổi nhãn). Số điện thoại là chuỗi thử nghiệm; `example.com` là tên miền dùng làm ví dụ.

Florence chạy 22 tác vụ mỗi device: ba ảnh thật × caption/caption chi tiết/detection/OCR, năm ảnh chữ × OCR/OCR có tọa độ. Quyet chạy 12 tình huống ảnh và 5 yêu cầu chữ, tổng 27 quyết định trong pipeline; thêm 5 yêu cầu với bản chép chữ chuẩn để chẩn đoán, tổng 22 requests/42 quyết định Quyet.

Nhãn ảnh chữ được ghi trước batch Florence; rubric/tình huống được ghi trước lần chạy Quyet. Ảnh đã được người thực hiện xem, có một lần smoke test SUP trước batch. Đây là **diagnostic**, không phải blind benchmark. Các tình huống dùng lại cùng ảnh, không phải mẫu độc lập. Chưa có ảnh lặn biển, ảnh mờ, QR code, số điện thoại viết bằng chữ, ảnh cấm hoặc dữ liệu thật từ người bán/review DANASEA.

Hai biến thể được lưu đầy đủ, cùng rubric và cùng nhãn kỳ vọng:

- `pipeline-baseline.json`: toàn bộ caption/detection gồm tọa độ và OCR thường.
- `pipeline-results.json`: bỏ tọa độ khỏi input Quyet; giữ caption và nhãn đối tượng; lấy chữ từng vùng từ OCR có tọa độ, bỏ token `<s>`/`</s>` và giữ xuống dòng.

Biến thể thứ hai được tạo **sau khi xem lỗi của baseline**, nên kết quả của nó cũng là diagnostic sau chỉnh sửa, không phải kiểm định độc lập. Rút gọn input giúp tốc độ nhưng không cải thiện đồng đều các quyết định. Không sửa nhãn kỳ vọng, không điều chỉnh threshold để làm đẹp kết quả.

## Sáu tính năng đã thử

“Khớp” nghĩa là nhãn chọn cao nhất khớp nhãn kỳ vọng; bao gồm `INSUFFICIENT` nếu tình huống yêu cầu từ chối kết luận. Không coi mọi lần abstain là quyết định sai an toàn, nhưng abstain vẫn không hoàn thành một tác vụ có bằng chứng rõ. Số đếm này không phải tỷ lệ chính xác của sản phẩm.

| Tính năng | Baseline | Input gọn + OCR từng vùng | Ý nghĩa hiện tại |
| --- | ---: | ---: | --- |
| Phân loại hoạt động trong ảnh | 1/3 | 1/3 | Quyet nhận kayak; trả `INSUFFICIENT` cho SUP và ô tô |
| Đối chiếu ảnh với loại dịch vụ | 1/5 | 2/5 | Vẫn báo ảnh SUP/SUP và kayak/kayak không liên quan |
| Đối chiếu ảnh với review | 1/4 | 1/4 | Có false acceptance, chưa dùng để duyệt tự động |
| Nhận diện thông điệp quảng cáo theo rubric demo | 3/5 | 4/5 | Phân biệt giảm giá với bảng thông tin thường chưa ổn định |
| Trích bộ số điện thoại/URL bằng regex | 3/5 | 5/5 | Giữ ranh giới dòng sửa lỗi URL dính chữ; chỉ là năm ảnh rõ nét |
| Đánh giá chính sách phần chữ bằng Quyet | 2/5 | 1/5 | Chưa đáp ứng; đây chưa phải kiểm duyệt đầy đủ hình ảnh |

Trên các ảnh có liên hệ, biến thể dùng vùng lấy đủ **3/3 số điện thoại và 2/2 URL**; hai ảnh không có liên hệ không phát sinh false positive trong bộ thử này. Regex chỉ nhận số theo mẫu Việt Nam và URL bắt đầu HTTP(S); chưa bao phủ email, domain trần, tài khoản Zalo, QR hoặc cách viết né kiểm duyệt.

Tác vụ phụ phát hiện lời mời thanh toán ngoài sàn khớp 3/5 ở baseline và 1/5 ở biến thể từng vùng. Khi đưa chữ chuẩn trực tiếp cho Quyet, quảng cáo khớp 5/5, thanh toán riêng 3/5 và chính sách chữ 2/5. Vì vậy sửa OCR không đủ để khắc phục toàn bộ lỗi ngữ nghĩa.

## Các lỗi cần giữ làm regression

1. **Florence thêm chi tiết không có thật.** Caption SUP chi tiết có “The shirt is white” dù người trong ảnh không mặc áo, và “There is nobody in the picture” dù chính caption đang mô tả một người. Nhãn OD là `surfboard`; không tự động đồng nhất nhãn này với SUP nếu thiếu dấu hiệu mái chèo/tư thế.
2. **OCR tiếng Việt mất dấu ngay trên ảnh chữ rõ.** CER sau chuẩn hóa khoảng trắng, dùng chữ từng vùng và bỏ special tokens: bốn ảnh Việt có khoảng **14.29–17.65%** lỗi ký tự; ảnh quảng cáo tiếng Anh có 0% ở cấu hình này. Đây không phải benchmark OCR tiếng Việt. Chưa sửa dấu bằng suy đoán vì có thể làm biến dạng bằng chứng.
3. **OCR thường làm dính dòng.** URL bị regex lấy thành `https://example.comChuyen` hoặc `https://example.comPay`. Dùng từng vùng giúp khôi phục ranh giới; không hardcode xóa riêng các từ trong bộ thử.
4. **Quyet báo ô tô liên quan review SUP.** Trong biến thể input gọn, `review-car-sup` trả `RELEVANT`, confidence 0.6502; caption ô tô của Florence là đúng. Đây là lỗi ở tầng quyết định với cấu hình hiện tại.
5. **Quyet suy quá mức từ ảnh.** Review nói ảnh chứng minh đã hoàn tiền 500 nghìn được trả `RELEVANT`, confidence 0.6793. Ảnh hoạt động không chứng minh hoàn tiền, thời gian, địa điểm hay danh tính người mua.
6. **Quyet nhầm thanh toán qua DANASEA là thanh toán riêng.** Có cả ở bản chép chữ chuẩn; không thể đổ toàn bộ lỗi cho OCR.
7. **Nội dung cố thay đổi nhãn chưa được xử lý ổn định.** OCR có lời mời chuyển khoản riêng nhưng Quyet vẫn chọn `NO` cho thanh toán ngoài sàn. Ca này không chứng minh đã có khả năng chống prompt injection.

Confidence là phân phối của model trong rubric này, chưa được hiệu chỉnh trên DANASEA; không dùng trực tiếp làm tỷ lệ đúng hoặc threshold duyệt bài.

## Tốc độ quan sát trên máy này

| Phần chạy | Median mỗi task/request | Khoảng quan sát | Ghi chú |
| --- | ---: | ---: | --- |
| Florence CPU, 4 threads | 1,469.22 ms | 928.72–2,083.53 ms | 22 task calls |
| Florence MPS | 570.95 ms | 210.49–3,168.32 ms | Task đầu tiên có khởi tạo kernel |
| Quyet CPU, input gọn | 78.95 ms | 47.19–120.91 ms | 22 requests, gồm oracle |

CPU và MPS cho **22/22 raw output giống nhau** trong batch Florence. Peak process RSS được Python báo khoảng 2,024.4 MiB ở CPU và 1,729.3 MiB ở MPS; RSS MPS không đại diện toàn bộ allocation của GPU. Đây là một lượt tuần tự, chưa phải benchmark có warmup/concurrency hoặc SLA. Các task có độ dài sinh khác nhau.

Median trên là **mỗi tác vụ**, không phải toàn bộ một ảnh. Một ảnh cần caption, detection và OCR sẽ cộng thời gian nhiều lần chạy. Ví dụ ảnh kayak ở MPS cần 1,673.61 ms cho bốn tác vụ trong lượt đã lưu; chưa gồm Quyet, upload, queue và ghi DB.

Các số trên loại trừ thời gian nạp model. Quyet cần 20.959 giây để nạp trong lượt input gọn; Florence cần 0.331 giây ở CPU và 0.822 giây ở MPS, chưa gồm import/runtime startup. CLI nạp lại model mỗi lần mở tiến trình. Nếu xây worker thực tế, cần giữ model trong bộ nhớ để tránh chi phí này.

## Chạy lại hoặc thử ảnh của bạn

Chạy lại bộ thử trên CPU:

```sh
sh /Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/experiments/florence-quyet/run.sh
```

Chạy batch Florence trên GPU Apple, rồi đánh giá Quyet từ JSON mới:

```sh
FQ_DIR=/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/experiments/florence-quyet
sh "$FQ_DIR/run.sh" vision --device mps
sh "$FQ_DIR/run.sh" decide --florence "$FQ_DIR/florence-mps.json" --output "$FQ_DIR/pipeline-from-mps.json"
```

Thử một ảnh local; thay đường dẫn ảnh ví dụ bằng ảnh của bạn:

```sh
FQ_DIR=/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/experiments/florence-quyet
sh "$FQ_DIR/run.sh" vision --image "$FQ_DIR/inputs/sup.jpg" --task all --device mps
sh "$FQ_DIR/run.sh" decide \
  --florence "$FQ_DIR/custom-vision-mps.json" \
  --activity SUP \
  --review 'Mình đã đứng chèo SUP trên biển.'
```

`--activity` và `--review` chỉ là ngữ cảnh do người dùng cung cấp. Nhánh phân loại chỉ nhận bằng chứng Florence, không nhận activity/review; nhánh quyết định chữ chỉ nhận OCR. Model không nhận nhãn kỳ vọng hoặc tên file ảnh. Kết quả riêng là `custom-result.json`; ảnh một lần chạy không ghi đè báo cáo batch. Nhánh custom trả `MANUAL_REVIEW` và các gợi ý để người xem đối chiếu, chưa cấp quyền xuất bản.

`--task` cũng nhận riêng `<CAPTION>`, `<MORE_DETAILED_CAPTION>`, `<OD>`, `<OCR>` hoặc `<OCR_WITH_REGION>`. Đặt token có dấu `< >` trong dấu nháy của shell. Dùng `--device cpu` nếu không dùng MPS. Chạy `vision --help`/`decide --help` để xem tham số.

Để tái tạo môi trường Florence mới, dùng Python 3.11 và `requirements.lock.txt` trong venv riêng. Chạy `prepare_model.py` bằng môi trường này để tải/check weights, rồi `prepare_inputs.py` để tải ảnh và dựng ảnh chữ (có mạng, font Arial macOS). Quyet giữ môi trường/weights ở experiment `quyet-small`; `run.sh` cho phép đổi biến `FLORENCE_PYTHON` và `QUYET_PYTHON`. Không cần bật API công khai hoặc nhập API key.

## Hướng đưa vào DANASEA sau khi có thêm đánh giá

1. **Trích xuất bất đồng bộ:** backend nhận ảnh, đưa ID ảnh vào queue; worker Florence trả caption, nhãn/boxes, OCR từng vùng, version model, lỗi/truncation. Lưu bản trích xuất cùng ảnh gốc để người kiểm duyệt xem bằng chứng.
2. **Backend xử lý tín hiệu chắc chắn:** chuẩn hóa số điện thoại/URL theo từng vùng OCR; lưu hit và vùng ảnh; đưa ảnh có liên hệ cần kiểm tra vào queue ngay cả khi Quyet trả `CLEAR` hoặc `INSUFFICIENT`. Có quảng cáo không tự động đồng nghĩa vi phạm.
3. **Quyet đưa gợi ý theo từng tác vụ:** giữ input gọn, tách phân loại ảnh khỏi metadata dịch vụ để giảm ảnh hưởng của nhãn cần kiểm tra; tách OCR khỏi yêu cầu diễn giải review. Cần thử lại rubric ngắn hơn, ngôn ngữ caption và model classification trên tập ảnh thật có nhãn. Các thử nghiệm hiện tại chưa phân biệt đầy đủ ảnh hưởng của tiếng Anh, rubric dài và kiến trúc model.
4. **Quyết định cuối có trạng thái thiếu bằng chứng:** dùng `SUGGESTED`, `NEEDS_REVIEW`, `EXTRACTION_FAILED`; không xóa/khóa tài khoản hay từ chối ảnh chỉ vì score model. Kiểm tra khớp loại hoạt động khác với xác minh ảnh thuộc giao dịch cụ thể.
5. **Đánh giá trước tự động hóa:** lấy ảnh thật đa dạng (kể cả lặn, cano, ảnh nhiều hoạt động, ảnh mờ/ít chữ, review không liên quan), cố định train/dev/test và rubric trước test; đo confusion matrix, precision/recall từng nhãn, false acceptance/false rejection, CER tiếng Việt, recall liên hệ và chi phí kiểm duyệt. Không dùng lại các ca vừa chỉnh input như một tập kiểm định mù.

Florence làm giảm hình ảnh thành mô tả/đối tượng/chữ, rồi Quyet xử lý phần đã trích xuất. Quyet không phục hồi chi tiết mà Florence bỏ sót hoặc đọc sai. Vì vậy cần đánh giá riêng nếu muốn suy luận không gian, đếm chính xác, đọc biểu đồ, đối chiếu nhiều ảnh, phát hiện nội dung cấm hoặc xác minh chứng từ. Bộ thử này chưa cung cấp bằng chứng cho các khả năng đó.

## Kiểm tra mã và nguồn ảnh

Đã chạy offline CPU/MPS, kiểm tra hashes, JSON, Ruff check/format, Python compile, shell syntax, Mypy với dependency imports bị bỏ qua, và Bandit mức medium/high. Ba cảnh báo tĩnh được review: `urlopen` chỉ dùng URL HTTPS cố định; hai `from_pretrained` chỉ nạp snapshot local đã xác minh. Suppression áp dụng đúng các call này, không tắt audit toàn bộ script. Các check này chứng minh runner thực thi được, không chứng minh độ đúng của quyết định AI.

Không thay đổi API/backend runtime trong experiment này. Các file tài liệu đang được người dùng chỉnh sửa không thuộc patch thử nghiệm.

Ảnh SUP: [Petar Milošević, Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Standup_paddleboarding.jpg), CC BY-SA 4.0. Ảnh kayak: [Surfsupusa, Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Sea_Kayak_Kealakekua.jpg), public domain. Ảnh ô tô: [mẫu documentation Hugging Face](https://huggingface.co/docs/transformers/model_doc/florence2), chỉ dùng làm đầu vào thử. Ảnh gốc giữ nguyên; overlay thêm bounding boxes và thu nhỏ phục vụ xem kết quả, giữ attribution nguồn. Không dùng ảnh mẫu làm tài sản thương mại DANASEA.
