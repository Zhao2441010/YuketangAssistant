import 'dart:io';

import 'package:dio/dio.dart';

import 'api_service.dart';
import '../utils/logger.dart';

/// 雨课堂答题图片上传（七牛）
class RCImageApi {
  static Future<String?> uploadImage(File imageFile) async {
    final tokenUrl = '/pc/generate_qiniu_token';
    final jsonData = {
      'bucket_name': 'cms-attachment',
      'expired_time': 3600
    };
    final tokenResponse = await ApiService.sendRequest(
      tokenUrl, method: 'POST', body: jsonData);
    if (tokenResponse == null ||
        tokenResponse.data['success'] != true ||
        tokenResponse.data['data'] == null) {
      logger.e('获取七牛 token 失败');
      return null;
    }

    final token = tokenResponse.data['data']['token'];
    final timestamp = DateTime.now().millisecondsSinceEpoch.toString();
    final originalFileName = imageFile.path.split('/').last;
    final fileName = '$timestamp$originalFileName';

    final formData = FormData.fromMap({
      'file': await MultipartFile.fromFile(imageFile.path, filename: fileName),
      'token': token,
      'key': fileName,
      'fname': originalFileName
    });
    final uploadResponse = await ApiService.sendRequest(
      'https://upload.qiniup.com/', method: 'POST', body: formData);
    if (uploadResponse == null ||
        uploadResponse.data['success'] != true) {
      logger.e('上传七牛失败');
      return null;
    }

    final key = uploadResponse.data['key'];
    return 'https://qn-scd1.yuketang.cn/$key';
  }
}
