let __MK_TOKEN_LIFETIME__ = 600;
let __MK_TOKEN_NAME__ = 'mkauthtoken';

function setTokenCookie(token){
    var date = new Date();
    date.setTime(date.getTime() + __MK_TOKEN_LIFETIME__ * 1000);
    let cookieInfo = __MK_TOKEN_NAME__ + '=' + token + ';expires=' + date.toUTCString() + ';path=/';
    return cookieInfo;
}

function getToken(cookieInfo){
    var value = document.cookie.match('(^|;) ?' + __MK_TOKEN_NAME__ + '=([^;]*)(;|$)');
    return value ? value[2] : null;
}

function removeTokenCookie(cookieInfo) {
    var date = new Date();
    document.cookie = __MK_TOKEN_NAME__ + "= ; expires=" + date.toUTCString() + "; path=/";
}