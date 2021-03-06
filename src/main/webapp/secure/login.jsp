<%@ page import="java.sql.SQLException" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="datamanagement.JDBC" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
    <link href="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/css/bootstrap.min.css" rel="stylesheet" id="bootstrap-css-login">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.css" type="text/css" rel="stylesheet">
    <meta charset="ISO-8859-1" content="">
    <title>Login</title>
    <style>
        Body {
            font-family: Calibri, Helvetica, sans-serif;
            background-color: white;
        }
        button {
            background-color: #05728f;
            width: 100%;
            color: white;
            padding: 15px;
            margin: 10px 0;
            border: none;
            cursor: pointer;
        }
        button a{
            color: white;
        }
        form {
            border: 3px solid #f1f1f1;
        }
        input[type=text], input[type=password] {
            width: 100%;
            margin: 8px 0;
            padding: 12px 20px;
            display: inline-block;
            border: 2px solid  #05728f;
            box-sizing: border-box;
        }
        button:hover {
            opacity: 0.7;
        }
        .cancelbtn {
            width: auto;
            padding: 10px 18px;
            margin: 10px 5px;
        }


        .container {
            padding: 50px;
            background-color: white;
            position:relative;
            width:fit-content;
            max-height: fit-content;

        }
        p{
            text-align: center;
        }

    </style>
    <link href="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/css/bootstrap.min.css" rel="stylesheet" id="bootstrap-css">
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/js/bootstrap.min.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.css" type="text/css" rel="stylesheet">

</head>
<body>
<%
    response.setHeader("Cache-Control","no-cache, no-store, must-revalidate");
%>
<div style="text-align: center;"> <h1> Login </h1> </div>
<div class="container">
        <%
            final JaasSecurity.GoogleOAuth helper = new JaasSecurity.GoogleOAuth();
            if (request.getParameter("code") == null || request.getParameter("state") == null) {

                out.println("<form action=\"j_security_check\" method=\"POST\">");
                out.println("<label>Username : </label>");
                out.println("<input type=\"text\" placeholder=\"Enter Username\" name= \"j_username\" required>");
                out.println("<label>Password : </label>");
                out.println("<input type=\"password\" placeholder=\"Enter Password\" name= \"j_password\" required>");
                out.println("<button type=\"submit\" value = \"Login\">Login</button>");
                out.println("</form>");
                out.println("");
                out.println("<form action=\"register\">" +
                        "    <button type=\"submit\">" +
                        "        Sign-Up" +
                        "    </button>\n" +
                        "</form>");
                out.println("<p>----------- or -----------</p>");
                out.println("<button><a href='" + helper.buildLoginUrl() + "'> Login using Google </a></button>");

                session.setAttribute("state", helper.getStateToken());
            } else if (request.getParameter("code") != null && request.getParameter("state") != null
                    && request.getParameter("state").equals(session.getAttribute("state"))) {

                JDBC db = null;
                try {
                    db = new JDBC();
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }

                String userinfo = helper.getUserInfoJson(request.getParameter("code"));
                String picture = JaasSecurity.GoogleOAuth.getValues(userinfo,"picture");
                String givenName =  JaasSecurity.GoogleOAuth.getValues(userinfo,"given_name");
                String name = JaasSecurity.GoogleOAuth.getValues(userinfo,"name");
                String email = JaasSecurity.GoogleOAuth.getValues(userinfo,"email");
                boolean createNew = true;
                try {
                    assert db != null;
                    ResultSet rs = db.dql("SELECT \"ID\" FROM public.\"USERS\"");
                    while (rs.next()){
                        if(rs.getString(1).equals(email)){
                            createNew = false;
                        }
                    }

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (createNew) {
                    try {
                        db.dml("INSERT INTO public.\"USERS\" VALUES ('" + email + "','" + name + "','" + picture + "','{}');");
                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }
                }

                    out.println("<img src=" + picture + "style=\"position: center\" alt=" + givenName + "hidden>");
                    out.println("<form action=\"j_security_check\" method=\"POST\">");
                    out.println("<input type=\"text\" value=\"" + email + "\" name=\"j_username\" hidden>");
                    out.println("<input type=\"password\" value=\"" + helper.getAccessToken() + "\" name= \"j_password\" hidden>");
                    out.println("<button  type=\"submit\" value =\"check\" hidden>Login in as " + givenName + " </button>");
                    out.println("</form>");
            }
            // TODO HTTP REQ TO SUBMIT
        %>
</div>
</body>
</html>