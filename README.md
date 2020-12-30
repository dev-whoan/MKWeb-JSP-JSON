
<img src="https://user-images.githubusercontent.com/65178775/83269009-6a481f80-a201-11ea-8475-0a779375a005.png" width="100%" />

# MKweb
Minwhoan - Kihyeon's JSP-Servlet Web Server Framework Repository

# MkWeb wiki
https://github.com/dev-whoan/MKWeb/wiki
OR
https://mkweb.dev-whoan.xyz

# Download Without Source
<a href="https://mkweb.dev-whoan.xyz/deploy.zip" target="_blank">Download</a>

# What is MKWeb?

MkWeb is well created 'Web-Server' framework.

We considered that developers should consider lots of things when he/she started to create some services; App, WebSite, science experimental, etc. Simply Front-End, and Back-End.

But actually, the simple service idea is about Front-End like: How about creating a food delivery service? People can choose the foods what they want to eat, and the page will show information about the food. Furthermore when they order it, the seller will receive the infor where consumer wants to receive it, and the other things about infor.

So the idea started with 'Developers just need to focus on the Front-End, easily View-side.'

However, there are lots of libraries for creating Web-Server, but when developer use it, he/she needs to construct, code, and test about it. It costs time too much, so we thought that 'What about let we solve the web-server part?'

So MKWeb borned.

We are designing our MKWeb with MVC pattern( however we are students and not pertty good at desining it, but we are working hard on how can we follow the pattern. ), and what MKWeb should offer to let developers focusing into there 'Front-End'.

<p align="center">
<img src="https://user-images.githubusercontent.com/65178775/81583650-9b94b300-93ec-11ea-8683-c4ffc67215f9.png" width="66%"/>
</p>
* Model

A server-side resources such as; DbAccessor, FileTransfer, Logger, ...

* Controller

A bridge of Model <---> Views

* Service

The actual functions(workers) which included in Controller

# JSON Configs
* Controller

In MkWeb, controller means all the relations and actions needed to define the relationship between Model and View such as page configs, sql configs, and etc, that 

Definition of Services and Controllers

JSON Configs are the biggest profit  when you use MKWeb.

You can easily access to Model via services or controllers.
To use the services and controllers, you need to set the config files, ~.json.

The configs are designed with JSON, so you can simply define it.

For example, before using MKWeb you need to design DBA, DBO, and whatever you need to access to DB, but now you just need to set relevant Configs to access DB.

Define the <span>SQL Json</span>, and View Json to choose which query to use, and after defining it, you can easily execute the SQL.

Just you need to define the relations <span>well</span> for request to response.

If the relations or requests are not defined, MkLogger would let you know what's going on.



# What MkWeb Can Do?

* Logging, DB Connect, and RESTful API

MKWeb supports Logging, DB connect, and limited RESTful API functions.

* MkLogger

MkLogger is logging every tasks on MkWeb, so you can check which requests has come, and what was the response.

You can manage your webserver easily with MkLogger's feedback.

For example, if the user asked wrong requesets, MkLogger would tell what is the problem,

And user asked right requests, and you set wrong definition(or relations) on controller, MkLogger would also tell the problem.

So, easy maintance.

Also you can use MkLogger on your custom java file so you can check your custom log on MkLogger.

With other frameworks, you need to connect DB with programming it, and create every DAO, DTO, Data Beans.

However, using mkweb, you don't have to create any DAO, DTO, and Data Beans.

You just need to config the View jsons and SQL jsons.

You can easily use your query result data with &lt;mkw:get&gt; HTML Custom Tag.

* MkWeb supports RESTful API.

For now, it's not 100% developed, but we are planning to support every function in RESTful API.

Supporting functions: <span>Method: Get, Post</span>

However, Get method is now limited supported. Following is now supporting functions.

For example, if you have Users data set, and there are 3 columns; name, phone, and address:

1. /users

2. /users/name/이름

3. /users/name/이름/phone/번호

==> 1. searching everything in users 2. searching with perfect condition (name have value, and phone have value)<br>

And following functions are not supporting now.

1. /users/name

2. /users/name/이름/phone

==> 1. want to get only name column that includes whole names in users / 2. want to get only phone column that the people whos' name is John.)

We support 100% POST method to create new data.

But there are some rules to use POST method, you can check it on RESTful API Configs.

What We Are Planning To

We are planning to following functions.

1. 100% RESTful API

2. Session

3. Device informations(Header)

# Developer E-mail

* dev-whoan
dev.whoan@gmail.com

* hyeonic
evan3566@gmail.com

* koh
khj1538104@gmail.com

# What we want you to do when you use MKWeb or copy and distribute MKWeb(convey).
* MKWeb을 사용하거나 복제, 배포할 때 해줬으면 하는 일

1. Don't forget who created it.
- Minwhoan, Kihyeon. We just wanted to be well known programmers.

2. Please scout us.
