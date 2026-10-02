import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_shapes.dart';
import '../theme/app_typography.dart';

class VendorTextField extends StatelessWidget {
  final String label;
  final String? hint;
  final TextEditingController? controller;
  final String? initialValue;
  final ValueChanged<String>? onChanged;
  final FormFieldValidator<String>? validator;
  final TextInputType keyboardType;
  final bool obscureText;
  final bool readOnly;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final String? helperText;
  final int maxLines;
  final bool isRequired;
  final String? suffixText;

  const VendorTextField({
    super.key,
    required this.label,
    this.hint,
    this.controller,
    this.initialValue,
    this.onChanged,
    this.validator,
    this.keyboardType = TextInputType.text,
    this.obscureText = false,
    this.readOnly = false,
    this.prefixIcon,
    this.suffixIcon,
    this.helperText,
    this.maxLines = 1,
    this.isRequired = false,
    this.suffixText,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(
              child: Text(
                label,
                style: AppTypography.labelMd(color: AppColors.onSurfaceVariant),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 4),
            if (isRequired)
              Text(
                'Bắt buộc',
                style: AppTypography.labelSm(color: AppColors.primary),
              )
            else if (readOnly)
              Text(
                'Chỉ đọc',
                style: AppTypography.labelSm(color: AppColors.tertiary),
              ),
          ],
        ),
        const SizedBox(height: 6),
        TextFormField(
          controller: controller,
          initialValue: initialValue,
          onChanged: onChanged,
          validator: validator,
          keyboardType: keyboardType,
          obscureText: obscureText,
          readOnly: readOnly,
          maxLines: maxLines,
          style: AppTypography.bodyMd(
            color: readOnly ? AppColors.onSurfaceVariant : AppColors.onSurface,
          ),
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: AppTypography.bodyMd(color: AppColors.outline),
            filled: true,
            fillColor: readOnly ? AppColors.surfaceContainer : AppColors.surfaceContainerLow,
            prefixIcon: prefixIcon != null
                ? Icon(prefixIcon, color: AppColors.secondary, size: 20)
                : null,
            suffixIcon: suffixIcon,
            suffixText: suffixText,
            suffixStyle: AppTypography.bodyMd(color: AppColors.onSurfaceVariant),
            contentPadding: EdgeInsets.symmetric(
              horizontal: 18,
              vertical: maxLines > 1 ? 14 : 14,
            ),
            border: OutlineInputBorder(
              borderRadius: maxLines > 1 ? AppShapes.radiusDefault : AppShapes.radiusFull,
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: maxLines > 1 ? AppShapes.radiusDefault : AppShapes.radiusFull,
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: maxLines > 1 ? AppShapes.radiusDefault : AppShapes.radiusFull,
              borderSide: const BorderSide(color: AppColors.secondary, width: 1.5),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: maxLines > 1 ? AppShapes.radiusDefault : AppShapes.radiusFull,
              borderSide: const BorderSide(color: AppColors.error, width: 1.5),
            ),
            helperText: helperText,
            helperMaxLines: 2,
            helperStyle: AppTypography.bodySm(color: AppColors.tertiary),
          ),
        ),
      ],
    );
  }
}
