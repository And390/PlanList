(function(){

    var planlist = this.planlist = {};

    // при загрузке страницы обработка всех элементов с указанными стилями
    planlist.load = function load()
    {
        //    обработать все элементы со стилем make_plan
        var elements = document.getElementsByClassName("make_plan");
        for (var i=0; i<elements.length; i++)  planlist.process(elements[i], true);

        //    добавить стили на страницу
        planlist.addStyles();
    };
    planlist.addStyles = function addStyles()  {
        var ulSelector = ".planlist";
        var selector = ulSelector+">";
        this.addCSS(
            ulSelector+"  {  margin: 0;  padding: 0 0 0 20px;  }\n" +
            selector+".mark  {  list-style: none;  }\n" +
            selector+".mark:before  {   content: '';  display: inline-block;  width: 16px;  height: 16px;  vertical-align: top;  margin-left: -20px;  margin-right: 4px;  background-repeat: no-repeat;  }\n" +
            selector+".mark:before  {  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH3wUBDDAiXNxiPAAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAABvSURBVDjL7ZMxDoAwDAPPTAz0pcx9Ukf4Br+AbyAxhIFOkApV3RCWvDiWI1mJzIwWdDTiqwG6MEpaMkdJchPM7EEgAnZjdL2FgM0JWD1vqYPB0UJNibOjTTUdBCABR2YCgufV2ylL6vOi3Z3/v8AJzpF4mtBt8iMAAAAASUVORK5CYII=');  }\n" +
            selector+".mark.detail:before  {  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAALlJREFUOI2tk7ENg0AMRR8pQklByRp07MIAWYAx2OA6dsgO7MAGlGmQ0KWIUb7AKAhi6Tff3/9s3x0xRhRABTyBTLjMuGqjF1EBdEA0tJJrhe+AwjNoRBSBGXgZ5lWu8QxSYFgJPQxAutTd+MYEjPyO0bSfkA5qOWUCAlAagnFLvpY6t83g3E7Y0bpk6RiUnlZ3cCr2DB4HuT8sUQwSoMffiaIHEu8d3IH8wNi5aTcjXH7K1z7T2e/8BiSyJIKAUP1UAAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.absent:before  {  background-color: #FF9090;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAAmJLR0QA/4ePzL8AAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQffAwkIIRMTJjSFAAAAHUlEQVQoz2NgGBGAkYGBgYHhP25ZJoZRwMDAwAAAKEABBN3F5lIAAAAASUVORK5CYII=');  }\n" +
            selector+".mark.done:before  {  background-color: #A0FFC0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABIQAAASEBDb+0VwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAEASURBVDiNlZOxSgNBGIS/mU0kpaARsfMNgoUgPoGPIFha+RrW2oiQRiy1CFjZCGJlo7W1CIKdjShisxZ3iXFzd7ldmGL3Z/6dmX+XGCM5AFaArck+k9wT4VH4HVjMbmB8LhyFo/FhVgPwwZhc4gPot5W+LfyTNIjGR23Ia8JvKblAeDANS9KCCCNgtaoeiRfzQhtW3+xPCLuTEAuZugbW/6SH/RryMzCYssiy8FNR1E15uCn8PUvWLbD0LyMR7pNkT4RfKxI/BsJMyMZnNVLH+IKw1zAlgvFlDfkF2Ggcc9mlIzRK/N4B/bnvZEpKV+iq9HsKdFq90sRPF8JOzgf7BTek35BTGuu2AAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.inwork:before  {  background-color: #F8F860;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAAmJLR0QA/4ePzL8AAAAJcEhZcwAAADsAAAA7AbhqDvIAAAAHdElNRQffAwkBBiUJiCj2AAABK0lEQVQoz4XRwUvTcRgG8M/WlujWZeTcRCmi0wgMEaYEgXTzL/AmmqdditBD7RREnTwEgWBE7NDFwwQPRt2KglAmCDIYCKHRoQ6CEMwfKN8Obm102XN5D+/zPjzP89IDl1oz5rK0eVlHzg17YFbDcYc4rWbDT8GmYQeCXQXZ9nrCd8Efb903Yk5Qc8uyKgkMue06UmLeIIXf8p7au/Cw6q4dBe9lXLPvpZybhnzyTh2mNH10R8WZyCvbztR8VZRsJyh6gSu+CCIVM+L6xLvjxiSNKqkLTpX/byPnuW8OBc98FkSWuu9T1qz4JTj32IAPgsjDDmVS8NqCphMxDNpoqbQwrmFMWfBDP8irCiJPLghJNzwSBKV/v7lqS7De1hiwaM09iS7jGUvSeuMvnt1TkaXun5YAAAAASUVORK5CYII=');  }\n" +
            selector+".mark.question:before  {  background-color: #C0C0C0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAHEAAABxABY5kuZQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADuSURBVDiNndI9SkQxFIbhJxduYWEnAzKIhcjYjztwBVZWgo0gYmMpuAQRrNzBoJ3L8GcNghYWgoioYDexuUWISQoDaZL3e3NyEjFG6cQCTnCPT7ziFsdY/MNn4TEeESvzGSstwV0Cv+AS1/hO1h+KAqwl0AfGyd5OVsmoJBjhZwCOsspWM8G0doVlbBQae5aE39AVBYVgh/Ps9MNqE7Nwj6skOMdp8xkzwX4S/sJ2kWsIbhLBVpVrCHbxjotWn8IAF0cIoYsxzqvA0OVaeAkHIYTNlqB1hfRbT2pcsYIQQo/1ZGnynwr28IQZ+hr3C9q/rzPyU2ffAAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.discard:before  {  background-color: #C0C0C0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAAmJLR0QA/4ePzL8AAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQffAwQVCAbcKad7AAAAyElEQVQoz53RMUoDYRDF8R8YE5TwmU4QK620du+xheANUnoSe7tle4uAwU48w14h2ihfExESUXCsZHdjl5lu3p+Z4T22rsP/o0Ity2qFC0s3XXGgEp1eC3ODFqiERilJrn0LC7vd5aExAefehKVQtEAtlODMq3DvUqhbIAsJp16EuaEk5E3gxLPwYMQmUAtTC+HRHij7JwrhU3iyDyaa/pNH3oUPV5Kk1AhVK481Qu4ZVf2ZtIMvB8YKM0PHVu5M3frp5zDaOtRf/3JQ8/kYFewAAAAASUVORK5CYII=');  }\n" +
            selector+".mark.frozen:before  {  background-color: #C0C0C0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAegAAAHoBlQypfwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAFUSURBVDiNpdO9alVREAXgb+MtEpJrmQuCCvoAgQuijY0Idj6CL2GRwiakSXUfICnzCFYJiKVExMoyKGgXEiwMAUmRZeGc5OTnpMmBgbNnZu29Zq+1WxJDX2vtLSSZDfWMBoBb+ILntT7BkyRvrjQnOQusYRnvEPyuSOWWsXYBU8AHGOMrNiu3WsBgtXKb1TPG/SRaEq21XTzEPu4meVTUPxTLl7X+gT+Y4GeSZx2DJWz3TnyFeexWzFeuq29jqRNgEd+q8Bd79X+I44rDyu1VTwqzCAtY6U6tXdcLeK/iGOtV69isYGGEUxxdEmeEhju1bq5KflTYwREOeiMcDI5QtCbY6V3SC8zhU8Vc5br6DiZ9GT/774V9jJM8Ltkuy/i9qE/wK8nTjsHUuZE2bjDShnMjTc+c2LPyrDYbsvIUsz6mXfcai/r7mhs+4nU3yoXe2z7nf9s42lma3vYyAAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.bury:before  {  background-color: #C0C0C0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAABAAAAAQABTdB3JwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADrSURBVDiNndI9SkNBFMXx330RRC1sxcLOIo2mEgvJErRwISIo2FhmA25CbV2JTRDcQLTXxq+xmeDLcyZ+XDhc5s05/+HNHSkl84RDTHBd2m8UKiKWImIvIlYxxBoGJe+8U1PWS+5jHGBjxlsBnLQAXb1h8SdAgz5OW8Gn3O/b3oXKb33gLiL6rc+b6GGl7S0CWrWd+2NKaVIyFKdQANzWDFVARKxjNy/H1SMqlzjEg68LfMZ+0VsIH+PV9/G946gKwDIuC8GuLtDMABC4+UV4qqsuYPSH8FRnOWvrH+Hpkx70cI6d6pjq1SB9AsgvGqNY2K9dAAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.warning:before  {  background-color: #FFB060;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAXQAAAF0BVWAulAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAEJSURBVDiNndKxSoNRDAXg77ZSJxG6uZSKm2tXnaTdfInOLtKncBJxcHJ3c+6qKB3s5uQDqIurUETiEuFX/7+ogQPJuSeHG5ISEZqilNKGiHhv0rSWNG9ijnnmfzPACdqJk0ZVRPwAhgjsJQLDWm1N8wrucVnhLpNb+Y3BARbYwnpiK7mDpQbo4gVHWc8wy/wo37rLDE7xjLWsr3CV+Vq+ndYaYBtvGFe4M5xV6nFqtusMprhDq8KNMKrUrdRMvxhgP1e1822kPvrfuJ3U7n9ecQcPuKjZyC1uaviL7OnABK/o1QgHGNTwveyZFDziGsdN19oQh9iF85zpPzgvEaGUsoHVP/5gERFPH8ESROSOmdB7AAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.burning:before  {  background-color: #FF9090;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAACyQAAAskBUqHwmAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAFISURBVDiNjZMvSINRFMV/5z5BwYUVURfEIAaL2IwGDQaTQcaaYDIrWMRmM5lNVhcNE5tmjQqiOBSbgrIw0F3DHuPbvr3phVvun3PPPfc93J1BDhiE9VR+iIRJmgFKEAqGrwFn/eosBWDYpmFl+Ll0WJCkfnVJBsC0Q9kwa9Ha8LhPzgbsXRHm0R+Bub61PY1jQheGHQISdpsBeQUKvQBdGhi2DVoGiu7ugptMugTM/6XBBIDDlsnGQSs9+byQ3SuE1QzlnIPti3ANtpfSwIQ9x4bvBFAdGM5pEBQO2urTjFxPndYScN/FGI7cvZlbQdiLsK845QGYjKymhDVivAGMpq7wCRTi9Kq7v8UBdVC8htfcvZFl1AFwOM7ESx3ZpSL4bLvGTpJXAEaEahmx7oTOhb3HC+z85yUGsF0RroR9CHsSqgKLqe/8C9wOHbpxUlYzAAAAAElFTkSuQmCC');  }\n" +
            selector+".mark.idea:before  {  background-color: #E8E8C0;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAEtgAABLYBZy9jPQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAFYSURBVDiNjZMxS0IBFIW/K7pYICiPJEFCx2rR0SkaBAmE/Bv+AP0H/gHHVgeHRnGpoK0haqghbIuCcBCDQmi4DR5F31PowoF7zz3nvvsu75m7sy3M7BTA3a+2aWIbTB0zC1SOBMwsMLNORB/eQOZf4BAoi74HnoGEu4/XDBqQBWrKi8AtMAPuhJm4ojQ1IOvuxDVnDARmtgsMgFfgANhR/xu4AAZmVgYCeeYbLAC0gRcgqXoIDJUn1WuvemJ675SedAL03f1HdUJAXF+apSempGVme0AGmKyc6F1YxATISNsys1Tc3adaHTN7AEorhhHrUQIe3f1z4Qnf4EwXL6jOATnlBfXqax4180CP+Yd1DXTFN4Gm8i5wI00PyC8HhLaoAFMgDRwLaXGVsD7yJeoWl8AR8CFqH3hy9/OIODxRA6uAh1DdpI38TIq3f3KbN9AWDeBLaGzT/QE1RP1OsyKqkAAAAABJRU5ErkJggg==');  }\n" +
            selector+".mark.future:before  {  background-color: #E0F0FF;  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAAbwAAAG8B8aLcQwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADJSURBVDiN1dMhTgNREIfx379QHAkKswJdS5MeAcsZ9gA9Ab0GF8D0EAhIkEVXoWvQFQXxMLuk3ZRmSYNgkhGTyffNe2/yUkpxTAyOov9ckKRKcp/kLckqyTzJVS9Bkks84h03uMUGrzuSUspOYoYlPnG3p/+A+XfdaU7xjBEucLpHMMZqqzZEjUUztepCHcEAH5i2ghpPuMbZIXhLUjUnnWkmj/uAHckIy3YLJ4fW+UOcY91e4QWTX0yfNEzdfcTSMxcNM8z//0xf2z3Z49QCAzwAAAAASUVORK5CYII=');  }\n" +
            ""
        );
    };
    window.addEventListener("load", planlist.load, false);  //указатель this теряется

    // обрабатывает указанный элемент
    planlist.process = function process(element, addClasses)
    {
        if (element==undefined)  element = document.body;
        else  element = this.getElement(element);

        var text = element.textContent;
        element.innerHTML = "";
        this.processText(text, element, addClasses);
        element.style.whiteSpace = 'pre-wrap';
    };
    planlist.processText = function processText(text, result, addClasses)
    {
        this.parse(text, new this.Parser (result, addClasses));
        return result;
    };

    var MARKER_CHARS_STRING = "!\"#$%&\'()*+,-./0123456789:;<=>?@[\\]^_`{|}~";
    var MARKERS = planlist.MARKERS = [];
    for (var i=0; i<255; i++)  MARKERS[i] = undefined;
    for (i=0; i<MARKER_CHARS_STRING.length; i++)  MARKERS[MARKER_CHARS_STRING.charCodeAt(i)] = null;
    MARKERS['-'.charCodeAt(0)] = "absent";
    MARKERS['+'.charCodeAt(0)] = "done";
    MARKERS['|'.charCodeAt(0)] = "inwork";
    MARKERS['='.charCodeAt(0)] = "inwork";
    MARKERS['?'.charCodeAt(0)] = "question";
    MARKERS['/'.charCodeAt(0)] = "discard";
    MARKERS['_'.charCodeAt(0)] = "bury";
    MARKERS['#'.charCodeAt(0)] = "frozen";
    MARKERS['*'.charCodeAt(0)] = "idea";
    MARKERS['!'.charCodeAt(0)] = "warning";
    MARKERS['^'.charCodeAt(0)] = "future";
    MARKERS['%'.charCodeAt(0)] = "burning";
    MARKERS[':'.charCodeAt(0)] = "detail";

    // получает на вход текст плана (в формате утилиты) и парсер для обработки событий (правельнее было бы parse-handler)
    planlist.parse = function parse(content, parser)
    {
        var spaces = [];
        //    обработать план
        for (var end=0; end<content.length; end++)  // end++ means skip '\n'
        {
            //    найти конец строки
            var i0 = end;
            end = content.indexOf('\n', end);
            if (end==-1)  end = content.length;
            //    найти пробелы в начале
            var i1 = skipSpaces(content, i0, end);
            //    проверить символ определяющий элемент списка и следующий пробел
            var c = content.charCodeAt(i1);  //может быть NaN или 10 ('\n'), если достингут конец строки
            var marker = MARKERS[c];
            //    если маркер распознан - это новый элемент списка
            if (marker!==undefined && content.charCodeAt(i1+1)<=32)
            {
                var listClosed = false;
                while (true)
                {
                    if (spaces.length==0 || i1-i0>spaces[spaces.length-1].length)  {
                        //    элемент нового дочернего или корневого списка
                        spaces.push(content.substring(i0, i1));
                        parser.openList();
                        break;
                    }
                    else if (i1-i0==spaces[spaces.length-1].length)  {
                        //    элемент текущего списка
                        break;
                    }
                    else  {
                        //    элемент родительского или соседнего списка
                        closeList(listClosed);
                        listClosed = true;
                    }
                }
                //    пропустить пробелы после маркера
                var i2 = skipSpaces(content, i1+1, end);
                //    добавить элемент
                parser.addItem(marker, i1);
                parser.addText(content, i2, end);
            }
            //    иначе строка текста
            else
            {
                //    закрыть предыдущие списки на нужное количество уровней
                // самый первый список может иметь нулевой отступ и надо отдельно проверить, что при пустом оступе закрываются все списки, включая корневой
                while (spaces.length && (i1==i0 || i1-i0<spaces[spaces.length-1].length))  closeList();
                //    добавить строку
                parser.addText(content, i1, end);
            }
        }
        //    закрыть открытые списки
        for (listClosed=false; spaces.length; listClosed=true)  closeList();

        //    вспомогательные функции
        // возвращает индекс первого непробельного символа в string начиная с index и до end (возвращает end, если нету)
        function skipSpaces(string, index, end)  {
            while (index<end && string.charCodeAt(index)<=32)  index++;
            return index;
        }
        // закрывает список
        function closeList()  {
            spaces.pop();
            parser.closeList();
        }
    };

    // парсер по умолчанию, который строит DOM плана
    // TODO prototype
    planlist.Parser = function Parser(element, addClasses)
    {
        // элемент всегда указывает на контейнер, куда будет добавляться текст,
        // кроме после вызова openList, в этом случае он указывает на <ul> до вызова createItem
        this.element = element;
        this.addClasses = addClasses;
        this.openList = function openList()  {
            var ul = document.createElement("UL");
            ul.className = "planlist";
            this.element.appendChild(ul);
            this.element = ul;
            return this.element;
        };
        this.closeList = function endList()  {
            this.element = this.element.parentNode.parentNode;
            this.spanOpened = false;
            return this.element;
        };
        this.addItem = function createItem(marker, pos)  {
            if (this.element.tagName!=="UL")  this.element = this.element.parentNode;  // sibling item
            var li = document.createElement("LI");
            if (this.addClasses)  li.className = 'mark ' + (marker ? marker : '');
            li.s = pos;
            this.element.appendChild(li);
            this.element = li;
            this.spanOpened = false;
            return this.element;
        };
        this.addText = function createText(source, start, end)  {
            if (this.spanOpened)  {
                var span = this.element.lastChild;
                span.textContent += source.substring(start, end===source.length ? end : end+1);  // with \n if possible
                span.e = end;
            }
            else  {
                span = document.createElement("SPAN");
                span.textContent = source.substring(start, end===source.length ? end : end+1);  // with \n if possible
                span.s = start;
                span.e = end;
                this.element.appendChild(span);
            }
            this.spanOpened = true;
            return span;
        };
    };


    // принимает id элемента или сам элемент и возвращает его
    planlist.getElement = function getElement(element)  {
        if (Object.prototype.toString.call(element).slice(8, -1)=="String")  {
            var elementId=element;  element = document.getElementById(elementId);
            if (element===null)  throw "Element not found: "+elementId;
        }
        else if (!this.isElement(element))  throw "Element or ElementID expected but found: "+element;
        return element;
    };

    // returns true if it is a DOM element (from http://stackoverflow.com/a/384380)
    planlist.isElement = function isElement(o)  {
        return  typeof HTMLElement === "object" ? o instanceof HTMLElement : //DOM2
            o && typeof o === "object" && o !== null && o.nodeType === 1 && typeof o.nodeName==="string";
    };

    // like http://stackoverflow.com/questions/524696/how-to-create-a-style-tag-with-javascript
    planlist.addCSS = function addCSS(css)  {
        var head = document.head || document.getElementsByTagName('head')[0];
        var style = document.createElement('style');
        style.type = 'text/css';
        if (style.styleSheet)  style.styleSheet.cssText = css;
        else  style.appendChild(document.createTextNode(css));
        head.appendChild(style);
    }

})();