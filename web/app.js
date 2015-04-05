
//var ajaxMessage;
//window.addEventListener("load", function()  {
//    ajaxMessage = getElement("ajaxMessage");
//});


function display(element, enable)
{
    element = getElement(element);
    if (enable===undefined)  enable = true;
    if (enable)  {
        element.style.display = 'block';
    }
    else  {
        element.style.display = 'none';
    }
}

/**
 * Выполняет ajax запрос, отображает результат или ошибку в указанном элементе для сообщения
 * Сервер должен вернуть либо успешное сообщение начинающееся с "success:",
 * либо ошибочное сообщение, начинающееся с "error:", остальные ответы считаются непредусмотренными.
 * @param url              URL запроса
 * @param content          тело POST-запроса (для передачи в http.post с авто-определением Content-Type)
 * @param [successText]    будет установлен в качестве текста сообшения при получении успеного ответа
 * @param [resultHandler]  обработчик успешного ответа, если установлен, включается асинхронный режим, принимает текст ответа в качестве параметра
 * @param [errorHandler]   обработчик ошибок, принимает ошибку в качестве параметра
 * @return {*} в асинхронном режиме: индентификатор экшена для карты ajaxAction.executing, в синхронном режиме:
 *     false, если сервер вернул ошибку или в процессе обработки или получения ответа произошла любая ошибка
 *     true, если сервер вернул пустое успешное сообщение
 *     и само успешное сообщение, если оно не пустое.
 */
function ajaxAction(url, content, successText, resultHandler, errorHandler)
{
    try
    {
        //    установить индикатор процесса
        var msgElem = getElement("ajaxMessage");
        msgElem.innerHTML = "...";
        changeClass(msgElem, "error", "success");
        display(msgElem);
        //    обработчики асинхронного режима
        var postResultHandler = resultHandler ? processResult : undefined;
        var postErrorHandler = resultHandler ? processError : undefined;
        //    выполнить запрос
        var response = http.post(url, content, null, postResultHandler, postErrorHandler);
        //    ответ
        if (resultHandler)  {
            //    в асинхронном режиме сгенерировать и сохранить ID экшена
            var actionID = ++ajaxAction.maxID;
            ajaxAction.executing[actionID] = true;
            return actionID;
        }
        else  {
            //    в синхронном режиме отобразить результат
            return processResult(response);
        }
    }
    catch (e)
    {
        //    отобразить возможную ошибку
        return processError(e);
    }

    function processResult(response)  {
            //    проверить, не прервана ли обработка
            if (actionID && !ajaxAction.executing[actionID])  {  console.log("ajaxAction.processResult: executing disabled, actionID: "+actionID);  return false;  }
        try  {
            //    ответ в лог
            if (response.startsWith("success:") || response.startsWith("error:"))  console.log(response);
            else  {  if (http.status!=404)  console.log(response);  }
            //    обработка ответа
            if (response.startsWith("success:"))  response = response.substring("success:".length);
            else if (response.startsWith("error:user:"))  throw response.substring("error:user:".length);
            else if (response.startsWith("error:client:"))  throw "Client request error";
            else if (response.startsWith("error:server:"))  throw "Internal server error";
            else if (response.startsWith("error:"))  throw "Error server response";
            else  throw "Wrong server response"+(http.status!=200 ? " ("+http.status+")" : "");
            //    display success result message
            msgElem.innerHTML = successText;
            //changeClass(msgElem, "error", "success"); меняется на success в начале
            //display(msgElem);
            //    вызвать обработчик, если установлен
            if (resultHandler)  resultHandler(response);
            //    пометить действие как завершенное
            if (actionID)  ajaxAction.executing[actionID] = false;
            //    вернуть ответ
            return response ? response : true;
        }
        catch (e)  {
            return processError(e);
        }
    }

    function processError(error)  {
        //    вывести в лог, если это не сгенерированная этой функцией ошибка (String)
        if (!error.typeOf("String"))  console.error(error);
        //    проверить, не прервана ли обработка
        if (actionID && !ajaxAction.executing[actionID])  {  console.log("ajaxAction.processError: executing disabled, actionID: "+actionID);  return false;  }
        //    отобразить сообщение
        msgElem.innerHTML = error.typeOf("String") ? error : "Error getting response";
        changeClass(msgElem, "success", "error");
        //display(msgElem);
        //    вызвать обработчик, если установлен
        if (errorHandler)  errorHandler(error);
        //    пометить действие как завершенное
        if (actionID)  ajaxAction.executing[actionID] = false;
        //    вернуть ответ
        return false;
    }
}
ajaxAction.maxID = 0;
ajaxAction.executing = {};