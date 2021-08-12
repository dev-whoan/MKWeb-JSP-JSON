<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <title>404 Not Found</title>
    <style>
        body {
            position: relative;
            width: 100%;
            height: 100%;
            margin: auto;
            text-align: center;
        }
        .wrap {
            position: relative;
            left: 50%;
            top: 50%;
            transform: translate(-50%, -50%);
        }
        .container {    
            position: relative;
            max-width: 500px;
            max-height: 500px;
            margin: auto;
            margin-top: 20%;
            margin-bottom: 1%;
            border: 1px solid #ccc;
            box-shadow: 3px 3px 3px #aaa;
            padding: 10px ;
        }
        
        .title {
            font-family: Fantasy;
            border-bottom: 1px solid #ccc;
            padding-bottom: 10px;
            font-size: 40px;
            font-weight: bold;
        }

        .footer {
            position: relative;
            margin: auto;
            padding: 10px ;
            max-width: 500px;
            max-height: 500px;
            border: 1px solid #ccc;
            box-shadow: 3px 3px 3px #aaa;
        }

        .footer_element {
            display: inline-block;
        }

    </style>
</head>

<body>
    <div class="wrap">

        <!-- header -->
        <div class="header"></div>

        <!-- section -->
        <div class="section">
            <div class="container">
                <h1 class="title">500 Internal Server Error </h1>
                <p class="">페이지에 문제가 있거나, 잘못된 접속 입니다. </p>
                <p>오류가 지속될 경우 관리자에게 문의해 주세요.</p>
            </div>
        </div>

        <!-- footer -->
        <div class="footer">
            <div>
                <p class="footer_element">MKWeb</p> 
                <p class="footer_element"> CopyRight &copy;</p> 
                <a class="footer_element">MKWeb.</a>  
            </div>
        </div>
    </div>
</body>
</html>