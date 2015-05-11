
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
        msgElem.innerHTML = error.typeOf("String") ? error : "Client script error";
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


// Настраивает для элемента, содержащего текст (например, span), превращение в поле с возможностью редактирования
//  и обратно при потере фокуса, нажатии Enter или Esc (соответственно с сохранением и без)
// Поддерживает многострочное редактирование (пользователю нужно использовать Ctrl+Enter)
function makeEditable(element, multiline, saveHandler, openHandler, closeHandler)
{
    element.onclick = function()  {
        var text = getInnerText(element);
        //    вызвать обработчик
        if (openHandler)  {
            var newText = openHandler(text);
            if (newText!==undefined)  text = newText;
        }
        //    make input
        generateEditable(text, text);
    };
    function generateEditable(text, sourceText)
    {
        //    создать поле ввода
        var input = document.createElement(multiline ? "TEXTAREA" : "INPUT");
        if (multiline)  {
            //    для textarea надо убрать управляющие элементы
            input.style.overflow = "hidden";
            input.style.resize = "none";
        }
        else
            input.type = "TEXT";
        //    установить текст
        input.value = text;
        //    скопировать шрифт
        var style = getComputedStyle(element);
        var savedDisplay = style.display;
        var savedPosition = style.position;
        var savedLeft = style.left;
        var savedTop = style.top;
        var savedZIndex = style.zIndex;
        //opcacity не трогаем - пусть input его затеняет
        var savedWhiteSpace = style.whiteSpace;
        input.style.fontFamily = style.fontFamily;
        input.style.fontSize = style.fontSize;
        input.style.lineHeight = style.lineHeight;
        //    скопировать позицию
        if (style.position=="absolute" || style.position=="relative" || style.position=="fixed")  {
            input.style.position = style.position;
            input.style.left = style.left;
            input.style.top = style.top;
        }
        input.style.float = style.float;
        input.style.display = style.display;
        //    для отступов задать значения отступов + границ + полей исходного текста
        //    минус границы и поля инпута для верхних точек (для центрирования, код идет после replace для корректности)
        input.style.marginLeft = (parseInt(style.marginLeft) + parseInt(style.borderLeftWidth) + parseInt(style.paddingLeft)) + "px";
        input.style.marginTop = (parseInt(style.marginTop) + parseInt(style.borderTopWidth) + parseInt(style.paddingTop)) + "px";
        input.style.marginRight = (parseInt(style.marginRight) + parseInt(style.borderRightWidth) + parseInt(style.paddingRight)) + "px";
        input.style.marginBottom = (parseInt(style.marginBottom) + parseInt(style.borderBottomWidth) + parseInt(style.paddingBottom)) + "px";
        //    заменить созданным полем исходный элемент
        insertAfter(input, element);
        //    только теперь getComputedStyle(input) будет содержать корректные значения
        var inputStyle = getComputedStyle(input);
        input.style.marginLeft = (parseInt(inputStyle.marginLeft) - parseInt(inputStyle.borderLeftWidth) - parseInt(inputStyle.paddingLeft)) + "px";
        input.style.marginTop = (parseInt(inputStyle.marginTop) - parseInt(inputStyle.borderTopWidth) - parseInt(inputStyle.paddingTop)) + "px";
        //    передать фокус, при потере фокуса вернуть
        input.focus();
        input.onblur = function()
        {
            restore(input.value, true, true);
        };
        function restore(text, save, close)  {
            if (save && text!=sourceText && saveHandler)  {
                var newText = saveHandler(text);
                if (newText!==undefined)  text=newText;
            }
            if (close && closeHandler)  {
                newText = closeHandler(text);
                if (newText!==undefined)  text=newText;
            }
            element.textContent = text;
            element.style.position = savedPosition;
            element.style.left = savedLeft;
            element.style.top = savedTop;
            element.style.whiteSpace = savedWhiteSpace;
            element.style.display = savedDisplay;
            element.style.zIndex = savedZIndex;
            removeNode(input);
        }
        //    поместить input за пределы видимости
        element.style.position = "absolute";
        element.style.left = (element.offsetLeft-parseInt(style.marginLeft))+"px";
        element.style.top = (element.offsetTop-parseInt(style.marginTop))+"px";
        element.style.zIndex = -1000;
        element.style.whiteSpace = "pre";
        element.style.display = "inline-block";
        //    менять ширину инпута при вводе
        input.oninput = function()
        {
            element.textContent = input.value + "\n";
            input.style.width = Math.floor(parseInt(style.width))+"px";    // element уже находится вне DOM, теоретически,
            input.style.height = Math.floor(parseInt(style.height))+"px";  // могут быть проблемы с ComputedStyle
        };
        input.oninput();
        //    еще события
        input.onkeydown = function(event)
        {
            //    нажали Esc - отмена
            if (event.keyCode==27)  {
                restore(sourceText, false, true);
            }
            //    нажали Ctrl+Enter - перенос на новую строку, если доступно
            else if (event.keyCode==13 && event.ctrlKey)  {
                //
                if (multiline)  {
                    input.value = input.value.substring(0, input.selectionStart) + "\n" + input.value.substring(input.selectionEnd);
                    input.oninput();
                }
            }
            //    нажали Enter - сохранение значения
            else if (event.keyCode==13)  {
                restore(input.value, true, true);
            }
        };
    }
}