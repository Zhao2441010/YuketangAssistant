import 'api_service.dart';
import '../models/user.dart';

/// 长江雨课堂登录接口（APP 端协议）
class RCLoginApi extends Api {
  RCLoginApi([super.user]);

  static Future<Map<String, dynamic>?> sendCaptcha(
      String phone, String ticket, String rand) async {
    final url = '/api/v3/user/code/send';
    final jsonData = {
      'phoneNumber': phone,
      'email': '',
      'ticket': ticket,
      'rand': rand
    };
    final response = await ApiService.sendRequest(
      url, method: 'POST', body: jsonData, userId: '');
    return response?.data;
  }

  static Future<Map<String, dynamic>?> verifyCaptcha(
      String phone, String code) async {
    final url = '/api/v3/user/code/verify';
    final jsonData = {
      'phoneNumber': phone,
      'email': '',
      'code': code
    };
    final response = await ApiService.sendRequest(
      url, method: 'POST', body: jsonData, userId: '');
    return response?.data;
  }

  /// 账号密码 / 短信验证码登录
  /// type: 1 手机号+密码，2 邮箱+密码，3 短信验证码
  static Future<Map<String, dynamic>?> login(
      int loginType, String account, String code, String ticket, String rand) async {
    final url = '/api/v3/user/login/app';
    final jsonData = {
      'type': loginType,
      'phoneNumber': '',
      'password': '',
      'email': '',
      'code': '',
      'pushDeviceId': ApiService.uniqueId,
      'ticket': ticket,
      'rand': rand
    };
    if (loginType == 2) {
      jsonData['password'] = code;
      if (account.contains('@')) {
        jsonData['email'] = account;
      } else {
        jsonData['type'] = 1;
        jsonData['phoneNumber'] = account;
      }
    } else if (loginType == 3) {
      jsonData['phoneNumber'] = account;
      jsonData['code'] = code;
    }
    final response = await ApiService.sendRequest(
      url, method: 'POST', body: jsonData, userId: '');
    return response?.data;
  }

  Future<User?> getUserInfo() async {
    final url = '/v/course_meta/user_info';
    final response = await ApiService.sendRequest(url, userId: user?.uid);
    if (response == null) return null;
    final userProfile = response.data['data']?['user_profile'];
    if (userProfile != null) {
      return User(
        uid: userProfile['user_id'].toString(),
        name: userProfile['name'] ?? '未知用户',
        avatar: userProfile['avatar']?.isNotEmpty == true
            ? userProfile['avatar']
            : (userProfile['avatar_96'] ?? ''),
        phone: userProfile['phone_number'] ?? '未知手机号',
        school: userProfile['school'] ?? '未知学校',
        platform: 'rainClassroom',
      );
    }
    return null;
  }

  // 扫码登录已改用网页端 WebSocket 协议，见 api/qr_login.dart。
}
