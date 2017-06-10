package me.chanjar.weixin.cp.api.impl.jodd;

import jodd.http.*;
import me.chanjar.weixin.common.bean.WxAccessToken;
import me.chanjar.weixin.common.bean.result.WxError;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.common.util.http.HttpType;
import me.chanjar.weixin.cp.api.WxCpConfigStorage;
import me.chanjar.weixin.cp.api.impl.AbstractWxCpServiceImpl;

public class WxCpServiceImpl extends AbstractWxCpServiceImpl<HttpConnectionProvider, ProxyInfo> {
  protected HttpConnectionProvider httpClient;
  protected ProxyInfo httpProxy;


  @Override
  public HttpConnectionProvider getRequestHttpClient() {
    return httpClient;
  }

  @Override
  public ProxyInfo getRequestHttpProxy() {
    return httpProxy;
  }

  @Override
  public HttpType getRequestType() {
    return HttpType.joddHttp;
  }

  @Override
  public String getAccessToken(boolean forceRefresh) throws WxErrorException {
    if (forceRefresh) {
      this.configStorage.expireAccessToken();
    }
    if (this.configStorage.isAccessTokenExpired()) {
      synchronized (this.globalAccessTokenRefreshLock) {
        if (this.configStorage.isAccessTokenExpired()) {
          String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?"
            + "&corpid=" + this.configStorage.getCorpId()
            + "&corpsecret=" + this.configStorage.getCorpSecret();

          HttpRequest request = HttpRequest.get(url);
          if (this.httpProxy != null) {
            httpClient.useProxy(this.httpProxy);
          }
          request.withConnectionProvider(httpClient);
          HttpResponse response = request.send();

          String resultContent = response.bodyText();
          WxError error = WxError.fromJson(resultContent);
          if (error.getErrorCode() != 0) {
            throw new WxErrorException(error);
          }
          WxAccessToken accessToken = WxAccessToken.fromJson(resultContent);
          this.configStorage.updateAccessToken(
            accessToken.getAccessToken(), accessToken.getExpiresIn());
        }
      }
    }
    return this.configStorage.getAccessToken();
  }

  @Override
  public void initHttp() {
    if (this.configStorage.getHttpProxyHost() != null && this.configStorage.getHttpProxyPort() > 0) {
      httpProxy = new ProxyInfo(ProxyInfo.ProxyType.HTTP, configStorage.getHttpProxyHost(), configStorage.getHttpProxyPort(), configStorage.getHttpProxyUsername(), configStorage.getHttpProxyPassword());
    }

    httpClient = JoddHttp.httpConnectionProvider;
  }

  @Override
  public WxCpConfigStorage getWxCpConfigStorage() {
    return this.configStorage;
  }
}