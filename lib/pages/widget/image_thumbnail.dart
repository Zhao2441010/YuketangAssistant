import 'dart:io';

import 'package:flutter/material.dart';

/// 通用图片预览与缩略图（长江雨课堂图片均为完整 URL）
void showImageViewer(
  BuildContext context, {
  File? file,
  String? url,
  String? heroTag,
}) {
  if (file == null && url == null) return;
  final Widget image = file != null
      ? Image.file(file, fit: BoxFit.contain)
      : Image.network(url!, fit: BoxFit.contain);
  showDialog<void>(
    context: context,
    builder: (context) => GestureDetector(
      onTap: () => Navigator.of(context).pop(),
      child: Container(
        color: Colors.black54,
        child: InteractiveViewer(
          minScale: 0.5,
          maxScale: 4,
          child: Center(
            child: heroTag != null ? Hero(tag: heroTag, child: image) : image,
          ),
        ),
      ),
    ),
  );
}

class ImageThumbnail extends StatelessWidget {
  const ImageThumbnail({
    super.key,
    this.file,
    this.url,
    this.thumbnailUrl,
    this.width = 80,
    this.height = 80,
    this.fit = BoxFit.cover,
    this.borderRadius = 8,
    this.onDelete,
    this.heroTag,
    this.overlay,
  });

  final File? file;
  final String? url;
  final String? thumbnailUrl;
  final double width;
  final double height;
  final BoxFit fit;
  final double borderRadius;
  final VoidCallback? onDelete;
  final String? heroTag;
  final Widget? overlay;

  void _preview(BuildContext context) {
    showImageViewer(context, file: file, url: url ?? thumbnailUrl, heroTag: heroTag);
  }

  @override
  Widget build(BuildContext context) {
    Widget image = _buildImage();
    if (heroTag != null) {
      image = Hero(tag: heroTag!, child: image);
    }
    return Stack(
      children: [
        GestureDetector(
          onTap: () => _preview(context),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(borderRadius),
            child: image,
          ),
        ),
        if (overlay != null) Positioned.fill(child: overlay!),
        if (onDelete != null)
          Positioned(
            right: 0,
            top: 0,
            child: GestureDetector(
              onTap: onDelete,
              child: const CircleAvatar(
                radius: 10,
                backgroundColor: Colors.red,
                child: Icon(Icons.close, size: 12, color: Colors.white),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildImage() {
    if (file != null) {
      return Image.file(file!, width: width, height: height, fit: fit);
    }
    final imageUrl = thumbnailUrl ?? url;
    if (imageUrl == null) {
      return SizedBox(width: width, height: height, child: const Icon(Icons.image));
    }
    return Image.network(
      imageUrl,
      width: width,
      height: height,
      fit: fit,
      loadingBuilder: (context, child, progress) => progress == null
          ? child
          : Container(
              width: width,
              height: height,
              color: Colors.grey.shade200,
              child: const Center(child: CircularProgressIndicator(strokeWidth: 2)),
            ),
      errorBuilder: (_, __, ___) => Container(
        width: width,
        height: height,
        color: Colors.grey.shade200,
        child: const Icon(Icons.broken_image),
      ),
    );
  }
}
