

//----------------        common        ----------------

var MAX_INT = 2147483647;
var MIN_INT = -2147483648;

//если пользоваться обычным eval, то из выполняемого скрипта видны все переменные функции из которой вызван eval!
function safeEval(script, object)
{
    if (object!=undefined)  with (object)  {  return eval(script);  }
    else  return eval(script);
}

//    from http://stackoverflow.com/questions/2735067/how-to-convert-a-dom-node-list-to-an-array-in-javascript
function toArray(obj)  {
    var array = [];
    // iterate backwards ensuring that length is an UInt32
    for (var i = obj.length >>> 0; i--; )  array[i] = obj[i];
    return array;
}

if (Object.prototype.typeOf===undefined)  Object.prototype.typeOf = function(targetType)
{
    var objectType = Object.prototype.toString.call(this).slice(8, -1);
    if (targetType===undefined)  return objectType;
    else  return objectType===targetType;
};

// from http://stackoverflow.com/questions/646628/javascript-startswith
String.prototype.startsWith = function (prefix)
{
    return this.substring(0, prefix.length) === prefix;
};

//  from http://stackoverflow.com/questions/280634/endswith-in-javascript
String.prototype.endsWith = function(suffix)
{
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

function cutIfStart(string, prefix)
{
    if (string.startsWith(prefix))  string = string.slice(suffix.length);
    return string;
}

function cutIfEnd(string, suffix)
{
    if (string.endsWith(suffix))  string = string.slice(0, -suffix.length);
    return string;
}

function cutString(string, prefix, suffix)
{
    if (!string.startsWith(prefix))  throw "String is not starts with "+prefix;
    if (!string.endsWith(suffix))  throw "String is not ends with "+suffix;
    return string.slice(prefix.length, -suffix.length);
}

function getString(string, fields)
{
    string = new String (string);
    for (var i in fields)  if (fields.hasOwnProperty(i))  string[i] = fields[i];
    return string;
}

function numEnding(num, nominativ, genetiv, plural)  {
    num = Math.abs(num);
    if (num%10==0 || num%10>=5 || num%100 >= 11 && num%100 <= 14)  return plural;
    else if (num%10==1)  return nominativ;
    else  return genetiv;
}

function escapeHTML(content)
{
    return content.replace(/[&<>]/g, function(key)  {
        return escapeHTML_replaceTable[key] || key;
    });
}
var escapeHTML_replaceTable = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;'
};


//----------------        стили        ----------------

function hasClass(element, className)  {
    var source = " "+ element.getAttribute("class") + " ";
    return source.indexOf(" " + className + " ") !== -1;
}

function removeClass(element, className)  {
    var source = " "+ element.getAttribute("class") + " ";
    var target = " " + className + " ";
    var i = source.indexOf(target);
    if (i!==-1)  source = source.substring(0, i+1) + source.substring(i+target.length);
    element.setAttribute("class", source.substring(1, source.length-1));
}

function addClass(element, className)  {
    var source = element.getAttribute("class");
    if ((" "+source+" ").indexOf(" "+className+" ") === -1)  element.setAttribute("class", source+" "+className);
}

// включает/выключает класс в зависимости от флага enable
function switchClass(element, className, enable)  {
    if (enable)  addClass(element, className);  else  removeClass(element, className);
}

// эквивалент:  if (hasClass(element))  removeClass(element, className);  else  addClass(element, className);
function toggleClass(element, className)  {
    var source = " " + element.getAttribute("class") + " ";
    var target = " " + className + " ";
    var i = source.indexOf(target);
    if (i!==-1)  source = source.substring(0, i+1) + source.substring(i+target.length);
    else  source += target.substring(1);
    element.setAttribute("class", source.substring(1, source.length-1));
}

// эквивалент:  removeClass(element, class1);  addClass(element, class2);
function changeClass(element, class1, class2)  {
    var source = " " + element.getAttribute("class") + " ";
    var target = " " + class1 + " ";
    var i = source.indexOf(target);
    if (i!==-1)  source = source.substring(0, i+1) + source.substring(i+target.length);
    if (source.indexOf(" "+class2+" ") !== -1)  return;
    source += class2;
    element.setAttribute("class", source.substring(1, source.length));
}

var getElementsByClassName = document.getElementsByClassName ?
	function(element, className)  {
		return element.getElementsByClassName(className)
	}
    :
	function(element, className)  {
        var result = [];
		var elements = element.getElementsByTagName('*');
		for (var i=0, len=elements.length; i!=len; i++)
            if (hasClass(element, className))  result.push(elements[i]);
		return result
	};

//    http://stackoverflow.com/a/16966533
function getStylesForSelector(selector) {
    for (var i=0, l=document.styleSheets.length; i<l; i++) {
        var sheet = document.styleSheets[i];
        var rules = sheet.cssRules ? sheet.cssRules : sheet.rules;
        if (!rules)  continue;
        for (var j=0, k=rules.length; j<k; j++) {
            var rule = rules[j];
            if (!rule.selectorText)  continue;
            var selectors = rule.selectorText.split(',');
            for (var m=0, n=selectors.length; m<n; m++)
                if (selectors[m].trim()===selector)  return rule.style;
        }
    }
    return undefined;
}


//----------------        DOM и HTML        ----------------

// returns true if it is a DOM node (from http://stackoverflow.com/a/384380)
function isNode(o)  {
    return  typeof Node === "object" ? o instanceof Node :
        o && typeof o === "object" && typeof o.nodeType === "number" && typeof o.nodeName==="string";
}

// returns true if it is a DOM element (from http://stackoverflow.com/a/384380)
function isElement(o)  {
    return  typeof HTMLElement === "object" ? o instanceof HTMLElement : //DOM2
        o && typeof o === "object" && o !== null && o.nodeType === 1 && typeof o.nodeName==="string";
}

function getElement(element)  {
    if (element.typeOf("String"))  {  var elementId=element;  element = document.getElementById(elementId);
                                      if (element===null)  throw "element not found "+elementId;  }
    else if (!isElement(element))  throw "element or elementId expected but found: "+element;
    return element;
}

function createNode(source)
{
    if (typeof source === "string")  return document.createTextNode(source);
    if (source instanceof Node)  return source;
    //    create element of specified tagName
    if (!source.tagName)  throw "No tagName parameter for createNode()";
    var element = document.createElement(source.tagName);
    //    set properties
    for (var i in source)  if (source.hasOwnProperty(i) && i!=='tagName' && i!=='childs')  element[i] = source[i];
    //    recursively create childs
    if (source.childs)  for (i=0; i<source.childs.length; i++)  element.appendChild(createNode(source.childs[i]));
    return element;
}

function fillupNodes(element)
{
    for (var i=1; i<arguments.length; i++)  element.appendChild(createNode(arguments[i]));
}

function removeNode(child)  {
    child.parentNode.removeChild(child);
}

function replaceNode(oldChild, newChild)  {
    oldChild.parentNode.replaceChild(newChild, oldChild);
}

function insertBefore(newChild, refChild)  {
    refChild.parentNode.insertBefore(newChild, refChild);
}

function insertAfter(newChild, refChild)  {
    refChild.parentNode.insertBefore(newChild, refChild.nextSibling);
}

function iterateNodes(element, handler)  {
    if (handler(element))  return true;
    for (var child=element.firstChild; child; child=child.nextSibling)  if (iterateNodes(child, handler))  return true;
    return false;
}

//    from http://stackoverflow.com/questions/1700870/how-do-i-do-outerhtml-in-firefox
function outerHTML(node)
{
    // if IE, Chrome, FF 11 take the internal method otherwise build one
    return node.outerHTML || (
        function(n)  {
            var div = document.createElement('div'), h;
            div.appendChild( n.cloneNode(true) );
            h = div.innerHTML;
            div = null;
            return h;
        }
    ) (node);
}

//    http://stackoverflow.com/a/6743966
function getInnerText(element)  {
    element = getElement(element);
    return element.innerText || element.textContent;
}

//    http://stackoverflow.com/questions/9340449/is-there-a-way-to-get-innertext-of-only-the-top-element-and-ignore-the-child-el
function getTopInnerText(element)  {
    element = getElement(element);
    var result = [];
    for (var child=element.firstChild; child; child=child.nextSibling)  if (child.nodeType==3)  result.push(child.data);
    return result.join("");
}

function getOffset(elem)
{
    // from http://javascript.ru/ui/offset

    var box = elem.getBoundingClientRect();

    var body = document.body;
    var docElem = document.documentElement;

    // Вычислить прокрутку документа. Все браузеры, кроме IE, поддерживают pageXOffset/pageYOffset,
    // а в IE, при наличии DOCTYPE прокрутка вычисляется либо на documentElement(<html>), иначе на body - что есть то и берем
    var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;

    // Документ(html или body) бывает сдвинут относительно окна (IE). Получаем этот сдвиг.
    var clientTop = docElem.clientTop || body.clientTop || 0;
    var clientLeft = docElem.clientLeft || body.clientLeft || 0;

    // Прибавляем к координатам относительно окна прокрутку и вычитаем сдвиг html/body,
    // чтобы получить координаты относительно документа
    // Для Firefox дополнительно округляем координаты
    var top = Math.round(box.top + scrollTop - clientTop);
    var left = Math.round(box.left + scrollLeft - clientLeft);
    var width = Math.round(box.width);
    var height = Math.round(box.height);

    return {  left: left, top: top, width: width, height: height, right: left+width, bottom: top+height  };
}

// скролл-позиция окна
function getScrollPos()
{
    // откуда и предыдущая функция + чуть более подробно здесь: http://learn.javascript.ru/metrics-window#прокрутка-страницы

    var body = document.body;
    var docElem = document.documentElement;

    var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;

    var clientTop = docElem.clientTop || body.clientTop || 0;
    var clientLeft = docElem.clientLeft || body.clientLeft || 0;

    return { top: Math.round(scrollTop-clientTop), left: Math.round(scrollLeft-clientLeft) }
}

//    from http://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393
// возвращает размеры текста (образуемого содержащим элементом прямоугольника) с указанными стилями
// обязательный только один параметр, можно передать текст и стиль либо элемент либо id элемента
// (в последних двух случаях текст и стиль берутся из элемента)
function getTextMetrics(text, style, addStyle)
{
    var element = undefined;
    if (text.typeOf("String"))  {
        if (!style)  {  element = document.getElementById(text);  if (element==null)  throw "element not found "+text;  }
    }
    else if (isElement(text))  element = text;
    else  throw "first parameter of getTextMetrics must be String or Element";
    if (!style)  style = getComputedStyle(element, undefined);
    if (element)  text = getElementText(element);

    if (text.length==0)  return { width: 0, height: 0 };

    var div = document.createElement('div');

    div.style.position = "absolute";
    div.style.left = "-1000px";
    div.style.top = "-1000px";
    var styles = ['fontSize', 'fontStyle', 'fontWeight', 'fontFamily', 'lineHeight', 'textTransform', 'letterSpacing'];
    for (var i=0; i<styles.length; i++)  if (style[styles[i]]!==undefined)  div.style[styles[i]] = style[styles[i]];  //условие для IE
    if (addStyle)  for (i in addStyle)  if (addStyle.hasOwnProperty(i))  div.style[i] = addStyle[i];

    div.innerHTML = text;
    document.body.appendChild(div);

    var result = {
        width: div.clientWidth,
        height: div.clientHeight
    };

    document.body.removeChild(div);

    return result;
}

// возвращает размеры элемента, если бы он находился вне родительских элементов и далеко от границ экрана
// (но с учетом рассчитанного стиля для себя и дочерних элементов)
// TODO getElementMetrics стоит получше протестировать на таблицах - в разных браузерах есть ошибка в несколько пикселей
function getElementMetrics(sourceElement, addStyle)
{
    var element = sourceElement.cloneNode(true);
    element.removeAttribute("id");
    element.style.position = "absolute";
    element.style.className = "";  //классы будут работать некорректно из-за контекста в котором они находятся, нужно скопировать нужные стили вручную
    element.style.left = "0";  //"-10000px";
    element.style.top = "0";  //"-10000px";
    element.style.width = "auto";  //опера еще и установленную ширину скопирует через встроенный стиль, поэтому это обязательно
    element.style.height = "auto";
    element.style.display = "inline";

    var styles = ['fontSize', 'fontStyle', 'fontWeight', 'fontFamily', 'lineHeight', 'textTransform', 'letterSpacing',
        'marginLeft', 'marginTop', 'marginRight', 'marginBottom', 'paddingLeft', 'paddingTop', 'paddingRight', 'paddingBottom',
        'borderLeftStyle', 'borderLeftWidth', 'borderTopStyle', 'borderTopWidth',  //целиком скопировать границу не получается в файрфоксе (нет рассчитанного свойства borderLeft при наличии borderLeftWidth и borderLeftStyle)
        'borderRightStyle', 'borderRightWidth', 'borderBottomStyle', 'borderBottomWidth'];
    function copyStyles(sourceElement, destinationElement)  {
        var sourceStyle = getComputedStyle(sourceElement, undefined);
        for (var i=0; i<styles.length; i++)  if (sourceStyle[styles[i]]!==undefined)  //условие для IE
            destinationElement.style[styles[i]] = sourceStyle[styles[i]];
        // как минимум в опере дефолтная граница у инпута получается "2px none" и уже не копируется
        if (sourceStyle.borderLeftStyle=="none" && sourceStyle.borderLeftWidth!="0" && sourceStyle.borderLeftWidth!="0px")  element.style.borderLeftStyle = "solid";
        if (sourceStyle.borderTopStyle=="none" && sourceStyle.borderTopWidth!="0" && sourceStyle.borderTopWidth!="0px")  element.style.borderTopStyle = "solid";
        if (sourceStyle.borderRightStyle=="none" && sourceStyle.borderRightWidth!="0" && sourceStyle.borderRightWidth!="0px")  element.style.borderRightStyle = "solid";
        if (sourceStyle.borderBottomStyle=="none" && sourceStyle.borderBottomWidth!="0" && sourceStyle.borderBottomWidth!="0px")  element.style.borderBottomStyle = "solid";
        //    теперь рекурсивно тоже самое для детей
        for (i=0; i<destinationElement.children.length; i++)  copyStyles(sourceElement.children[i], destinationElement.children[i]);
    }
    copyStyles(sourceElement, element);
    if (addStyle)  for (i in addStyle)  if (addStyle.hasOwnProperty(i))  element.style[i] = addStyle[i];

    document.body.appendChild(element);

    var box = element.getBoundingClientRect();
    var result = {
        width: box.width,  //element.offsetWidth,
        height: box.height  //element.offsetHeight
    };


    document.body.removeChild(element);

    return result;
}

function fireEvent(element, event)
{
    if (document.createEventObject)  {
        // dispatch for IE
        var evt = document.createEventObject();
        return element.fireEvent('on'+event, evt)
    }
    else  {
        // dispatch for firefox + others
        var evt = document.createEvent("HTMLEvents");
        evt.initEvent(event, true, true ); // event type,bubbling,cancelable
        return !element.dispatchEvent(evt);
    }
}

function replaceAddress(url)
{
    if (!!(window.history && history.replaceState))  history.replaceState(null, null, url);
}