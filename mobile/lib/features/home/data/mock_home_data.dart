import '../domain/models/home_models.dart';

class MockHomeData {
  MockHomeData._();

  static const String heroImageUrl =
      'https://lh3.googleusercontent.com/aida-public/AB6AXuCkT_Mh43TcyYMVWVAH0Q2n_8aDg2hzAyX9CGsmxncw-7TFz7vO0tjVnNk9iTa92odRIOf8-wRQcMtQLEfvUMoWS3yKoN9KbAP4qmbdtUAoqZzaZdWP-wPj9MjeS622HIILwGrAchqslsuqPrermX34898sw3v2rnEu7ZfEvi1hfnt51lLSixqCIQcvJH1ALnmBm6q76J91LoJZwcmVn_-w-xOv9vdXJrDOuZXvhf8MEv0wXBAr6dYO8A';

  static const List<ExperienceItem> featuredExperiences = [
    ExperienceItem(
      id: 'exp-1',
      title: 'Chèo SUP đón bình minh',
      location: 'Bãi biển Mỹ Khê',
      duration: '2 giờ',
      rating: 4.9,
      reviewCount: 128,
      price: 280000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuAHQZofgP_5jp_1vY3guY8EmpWWnRmw3ak7UKqvHgGFW7O5D-TQd5FhoxfCe11bxElfI-pOasTJxGjinYnVSc1dopFqZmXvfBvUZFW7-KYNvrNMuBQU0Cd_prDBTx3cqcblq-s4Y35Wl35k52E6UEytQ8d7iro22xpo4SW_zSau-xVE5Ta_-UPWzfeDOAmA0yPKi-2oMxHp9EiOMv5Enf-HLHsOpWtKnGRZHcK5wPccRk6Nt9qTFjY_QQ',
      isVerified: true,
      isFavorite: true,
    ),
    ExperienceItem(
      id: 'exp-2',
      title: 'Khám phá biển bằng cano',
      location: 'Mũi Nghê - Sơn Trà',
      duration: '3.5 giờ',
      rating: 4.8,
      reviewCount: 94,
      price: 450000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuCiHITgvmCfXOpW3RW3QfqiN_HQcuixSE5dM8u6Rs0lkaGpjm4ENFh5edkXQwKdhFUoRjxX5k1fX86vWSik5aMqUA5XbzKd5P3NpzJgHMNGpmq1elXttpjoWp_lgCB5_97cOPBSM_-bWy0-oKdfkh9Xzcn_rK9SYV1FjSoxdHTpaS3sW0yfj9JbaAgg6QiF25XGLHkGIafHrDPMhY1uN7JJO7-pv3NAvA4YDPObE917YlVzGseBx1xf0A',
      isVerified: true,
      isFavorite: false,
    ),
    ExperienceItem(
      id: 'exp-3',
      title: 'Trải nghiệm lặn biển',
      location: 'Bán đảo Sơn Trà',
      duration: '2.5 giờ',
      rating: 4.9,
      reviewCount: 160,
      price: 520000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuCQiemqmFohmH6vp4J5-GU93ZVv5o3zN950SuDNYGURkdTfuBACsZMy1efpM5S2ujF1V8e-U4OvnOqTy84TbHDk37OGzwzpbQPTANsmxiRQG0Nelqyfpe0hs75zxyYa1_d-XiaPWWDjEMHMpGGNAOUx0aaoCl5L9tzPUM4Yu5v8w--ztP8Ld7RXADx9TDWqbpLYFGeNpAwT_PF0bp51PE7nbZHPo1DX_NP0fUtuTDgsAsZEk2h9Q8suEw',
      isVerified: true,
      isFavorite: false,
    ),
    ExperienceItem(
      id: 'exp-4',
      title: 'Chèo kayak cùng nhóm bạn',
      location: 'Bãi Rạng - Sơn Trà',
      duration: '2 giờ',
      rating: 4.8,
      reviewCount: 72,
      price: 220000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuAx4Itxvo6j84lXZio1yIzBV6FMDuxQHaDCp5ZVlmD6saJQ84RHUGcVT5kTez8awpH-3r_oPnx3Hz30nhhKjD8y-j4WbX_i3Sb_2s1rJSRck8ovswNqFLZHmrJSxJ_Vwv0o61xkxcneoqGr8KrHmPxmegINYHaEOTqUyOj_Nx971Kxhy3hFhkblMcA2JgkGnwWUwVAXqC3JGFLrAX_3ogPMsnnXIWVUAEmVOGao3st3OuKy7z-XdvhR0g',
      isVerified: true,
      isFavorite: false,
    ),
  ];

  static const List<BeachRegionItem> beachRegions = [
    BeachRegionItem(
      id: 'region-1',
      tag: 'Khu vực sôi động',
      title: 'Bãi biển Mỹ Khê',
      description: 'Bình minh, sóng êm và hoạt động chèo SUP ven bờ',
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuDV81sr-cfMbXjSHAbpwyvJdNNUs-0X5jHwHFmhk685uCGmkTWc0AGBhrnJnuthEArhcEQelYJvMZRWSlALGasNbjO5yCvwk8YhRbnqGw1dnjtHXk7zwxRGgdSgmXLS4JKY-Mv6Dj0mioIwGyx5_L5pWjo3RMafIMLCRRHy9HqAUjFljKAc1FKUOhOXABzZrZrC7sHRrKZwKNG1AHJFT_7_7I0OAAzopZZjK3kgBv6MxnTTRfmO_NY5pw',
    ),
    BeachRegionItem(
      id: 'region-2',
      tag: 'Thiên nhiên hoang sơ',
      title: 'Bán đảo Sơn Trà',
      description: 'Vùng nước xanh ngọc bích, rạn san hô tự nhiên',
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuD5MAlKHUvy5GgSUT10qGNJ_oHQfIThKCEj-qRXWzz0lRZwLqXT1UWH5avko3IIUo74-9sJCsK-eXYegcR02lpyF-AzDHe8260Wmj062hPUIkKWaSK3tLOb3zGe-A1u-ZC7jP8VoOLN0q9FytTQBdLhF5pVLS201cKDfBmLUqdsErp4rA9yhRFj4HLYfYh1ToRlO-vki6-AGsB_XVwvv7TTorb9WQ3iFsFR1GykkdjqvXQh8iV5XreQUg',
    ),
    BeachRegionItem(
      id: 'region-3',
      tag: 'Thư thái & riêng tư',
      title: 'Bãi biển Non Nước',
      description: 'Không gian thoáng đãng cho kayak và lướt sóng nhẹ',
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuB3aqWHNNtQYZIiV0mMYu3yeZaHkX34BUdrhP7hr1-32t5hOiTt_GNPCk-66fvE-P8-YUFEA8I7b0iLyRgdg_cazmXOhktLV4-gM_UTaDoKHvhydx-E61na5GhPcIw6_Z5cB9XrVrhTbmT6tLJLP5zSHb6S2QxvB_ApSDOyS0pVShLCDCawTOM7B83DCtnk7vCv6MHFucDyKpeDlCLuggRKW_y80mUcc4AU_cKbOIeYu_2nk5V7vPfT_w',
    ),
  ];

  static const WeatherOceanData myKheWeather = WeatherOceanData(
    location: 'Bãi biển Mỹ Khê',
    updatedAt: 'Cập nhật 06:00',
    windSpeed: '12 km/h',
    windDesc: 'Gió cấp 2',
    waveHeight: '0.4 m',
    waveDesc: 'Sóng êm',
    rainLevel: '0 mm',
    rainDesc: 'Không mưa',
    safetyTip:
        'Điều kiện biển thuận lợi cho SUP và lướt cano. Luôn mặc áo phao và tuân thủ cờ hiệu cứu hộ dọc bãi biển.',
  );

  static const WeatherOceanData sonTraWeather = WeatherOceanData(
    location: 'Bán đảo Sơn Trà',
    updatedAt: 'Cập nhật 06:00',
    windSpeed: '9 km/h',
    windDesc: 'Gió nhẹ',
    waveHeight: '0.2 m',
    waveDesc: 'Mặt nước phẳng',
    rainLevel: '0 mm',
    rainDesc: 'Nắng nhẹ',
    safetyTip:
        'Thích hợp nhất cho lặn ngắm san hô Bãi Bụt và Bãi Rạng. Chú ý giữ gìn vệ sinh môi trường biển san hô.',
  );

  static const List<RecentItem> recentItems = [
    RecentItem(
      id: 'rec-1',
      title: 'Chèo SUP Mỹ Khê',
      location: 'Mỹ Khê',
      price: 280000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuCXM8nlhaKrVL59fIa5VwAERnEnYBx7TrvuXdygMzAqs1p1JQa6NSv9JXviCWLynWM0PYl1CmfW7DAGBx-klK7eefFDEfaNC4gdgsAbf158c00q-PwkL8VPo8By0FX5ySbNM-aLwfxIIfjuPxZZhZiVVh_B-rxy8ntXQdjr0hyTcpIh9KAm0j3Z7_YrehkVjC5IaXFX1Jg052LjVUkl2eVK_X2i7hIWK-3oiqnPjqCQ8hydfraDr4IP4A',
    ),
    RecentItem(
      id: 'rec-2',
      title: 'Lặn biển Sơn Trà',
      location: 'Sơn Trà',
      price: 520000,
      imageUrl:
          'https://lh3.googleusercontent.com/aida-public/AB6AXuDk3xnZC_-eFnFX4FO_g0GsaKSEkVA7Zaf8VuzqEW50ND2BdFasbXjUh-vpiCFZiI2Va5BVHaBCspTy780okn7UyZb0FjsXTIq_Mz5odaRNJ2oYIJNjHLJiFC9IXP9r4VWidBHKaB_BG-m6XiWkE3yJoxmVp0otSl0S_an3Ww2l5MVtX61t4E3jhaGdPcJHHJLvbtkG2afwig4n0yDNc895m68-UN19-Cet7pxuNXpfyzXhl8cMBjMrhQ',
    ),
  ];
}
