
//    AJAX

var http =
{
    createRequest: function createHttpRequest()  {
        try  {  return new XMLHttpRequest();  }
        catch (e)  {
            try  {  return new ActiveXObject("Microsoft.XMLHTTP");  }
            catch (e)  {  return new ActiveXObject("Msxml2.XMLHTTP");  }
        }
    },

    status: undefined,

    // выполняет http-запрос
    // если указан handler, то асинхронный
    execute: function (method, url, content, contentType, handler, errorHandler)
    {
        this.status = undefined;
        //    создать объект запроса
        var httpRequest = this.createRequest();
        //    открыть
        httpRequest.open(method, url, handler!==undefined);
        //    установить обработчики
        if (handler!==undefined)  httpRequest.onreadystatechange =  function()  {
                if (httpRequest.readyState == 4 && httpRequest.status != 0)  {
                    handler(httpRequest.responseText, httpRequest.status);
                }
            };
        if (errorHandler)  httpRequest.onerror = errorHandler;
        //    установить Content-Type (если не указан, установит браузер)
        if (contentType)  httpRequest.setRequestHeader("Content-Type", contentType);
        //    отправить запрос
        httpRequest.send(content===undefined ? null : content);
        //    если установлен обработчик - больше ничего делать не надо
        if (handler!==undefined)  return httpRequest;
        //    если нет - сохранить статус и вернуть текст ответа
        this.status = httpRequest.status;
        return httpRequest.responseText;
    },

    // далее функции-обертки

    // GET-запрос
    get: function (url, resultHandler, errorHandler)
    {
        return this.execute("GET", url, undefined, undefined, resultHandler, errorHandler);
    },

    // POST-запрос
    // Значение null для contentType является маркером для обработки content и автоматического определения Content-Type
    post: function (url, content, contentType, resultHandler, errorHandler)
    {
        // определить Content-Type
        if (contentType===null && content!==undefined && content!==null)
            if (content instanceof ArrayBuffer || content instanceof Blob)
                contentType = "application/x-binary;charset=x-user-defined";
            else if (content.typeOf("String"))
                contentType = "text/plain;charset=UTF-8";  //браузер все равно перепишет кодировку в UTF-8
            else  {
                //    make query string
                function appendQuery(name, value)  {
                    if (query.length)  query.push('&');
                    query.push(name, '=', value ? encodeURIComponent(value) : value);
                }
                var query = [];
                for (var i in content)  if (content.hasOwnProperty(i))  {
                    if (content[i].typeOf("Array"))
                        for (var pi=0; pi<content[i].length; pi++)  appendQuery(i, content[i][pi]);
                    else
                        appendQuery(i, content[i]);
                }
                content = query.join('');
                contentType = "application/x-www-form-urlencoded;charset=UTF-8";  //тут видимо тоже
            }
        // выполнить запрос
        return this.execute("POST", url, content, contentType, resultHandler, errorHandler);
    }
};

