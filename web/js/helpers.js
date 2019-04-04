function getUrlParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    let regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

function getUrlQueryVariable(variable)
{
    let query = window.location.search.substring(1);
    let vars = query.split("&");
    for (let i=0;i<vars.length;i++) {
        let pair = vars[i].split("=");
        if(pair[0] == variable){return pair[1];}
    }
    return(false);
}

function arrayToObject(ary, keyName) {
    const obj = {};
    for (let v of ary) {
        obj[keyName] = v;
    }
    return obj;
}

/**
 * @param {Map} map
 * @return object
 */
function mapToObject(map) {
    return [...map.entries()].reduce((pre, [k, v]) => (pre[k] = v, pre), {});
}

/**
 * Create an HTMLElement with specified tag name, attributes and property values.
 * @param {string} tag
 * @param {Object<string,*>} attributes setAttribute(key, value) will be called for each attribute. To set attribute name only, set value to ''.
 * @param {Object<string,*>} properties HTMLElement[key] = value will be called for each property.
 * @return {HTMLElement} The created element.
 */
function createElement(tag, attributes = null, properties = null) {
    let ele = document.createElement(tag);
    if (attributes) {
        for (let key of Object.keys(attributes)) {
            ele.setAttribute(key, attributes[key] == null ? '' : attributes[key]);
        }
    }
    if (properties) {
        for (let key of Object.keys(properties)) {
            //supports setting nested property, e.g. setting style.fontSize.
            let prop = ele;
            let splitAry = key.split('.');
            for (let i=0; i<splitAry.length; i++) {
                let p = splitAry[i];
                if (i === splitAry.length - 1)
                    prop[p] = properties[key];//set value when it's the final property
                else
                    prop = prop[p];//save the prop for the next iteration
            }
        }
    }
    return ele;
}

HTMLElement.prototype.getPropRecursive = function (prop) {
    return this[prop]
};



/**
 * Create and append an HTMLElement with specified tag name, attributes and property values.
 * @param {string} tag
 * @param {Object<string,string>} attributes
 * @param {Object<string,Object>} properties
 * @return {HTMLElement} The parent element.
 */
HTMLElement.prototype.createElement = function (tag, attributes = null, properties = null) {
    this.appendChild(createElement(tag, attributes, properties));
    return this;
};

/**
 * @param str
 * @return {HTMLElement | HTMLCollection}
 */
function parseHTMLElement(str) {
    let ele = document.createElement('template');
    ele.innerHTML = str;
    return ele.content.childElementCount === 1 ? ele.content.firstElementChild : ele.content.children;
}

/**
 * Go up through the DOM tree and find the first element with the specified class.
 * @param {string} className
 * @return {HTMLElement | null}
 */
HTMLElement.prototype.getParentByClass = function (className) {
    let parent = this.parentElement;
    if (parent == null) return null;
    else {
        if (parent.classList.contains(className)) return parent;
        else return parent.getParentByClass(className);
    }
};

/**
 * Get the compare function based on the property supplied.
 * @param {string} prop
 */
function getCompareFunc(prop) {
    return (a, b) => a[prop] < b[prop] ? -1 : (a[prop] > b[prop] ? 1 : 0);
}

HTMLElement.prototype.clearChildNodes = function () {
    while (this.lastChild) {
        this.removeChild(this.lastChild);
    }
};