/**
 * retrieves all submitted parameters
 */
function getParameters(obj) {
	// alert("now in get method");
	var getstr = "?";
	for (i = 0; i < obj.elements.length; i++) {
		if (obj.elements[i].tagName == "INPUT") {
			if (obj.elements[i].type == "text") {
				getstr += obj.elements[i].name + "=" + obj.elements[i].value
						+ "&";
			}
			// we can delete this code as not using any checkboxes or radio
			// buttons neither select tag
			if (obj.elements[i].type == "checkbox") {
				if (obj.elements[i].checked) {
					getstr += obj.elements[i].name + "="
							+ obj.elements[i].value + "&";
				} else {
					//getstr += obj.elements[i].name + "=&";
				}
			}
			if (obj.elements[i].type == "radio") {
				if (obj.elements[i].checked) {
					getstr += obj.elements[i].name + "="
							+ obj.elements[i].value + "&";
				}
			}
		}
		if (obj.elements[i].tagName == "SELECT") {
			var sel = obj.elements[i];
			getstr += sel.name + "=" + sel.options[sel.selectedIndex].value
					+ "&";
		}

	}
	// alert("get method returns:" + getstr);
	return getstr;
}
/**
 * generates request
 */
function createXHR() {
	http_request = false;
	if (window.XMLHttpRequest) { // Mozilla, Safari,...
		http_request = new XMLHttpRequest();
		if (http_request.overrideMimeType) {
			// set type accordingly to anticipated content type
			// http_request.overrideMimeType('text/xml');
			http_request.overrideMimeType('application/json');
			// alert(http_request.context);
			// alert("json");
		}
	} else if (window.ActiveXObject) { // IE
		try {
			http_request = new ActiveXObject("Msxml2.XMLHTTP");
			// alert("ie");
		} catch (e) {
			try {
				http_request = new ActiveXObject("Microsoft.XMLHTTP");
				// alert("not ie");
			} catch (e) {
			}
		}
	}
	if (!http_request) {
		alert('Cannot create XMLHTTP instance');
		return false;
	}
	return http_request;
}
/**
 * loads json tree with results (calls method from example3 rgraph...)
 */
function loadTree(treeData) {

	init(treeData);
}
/**
 * 
 */
function loadParticularResult() {
	document.getElementById('right-container').innerHTML = '';
	document.getElementById('mycanvas-label').innerHTML = '';
	var finalResult = "";
	var obj = document.getElementById('myform');
	var parameters = getParameters(obj);
	var serviceName = 'service/' + 'results';
	var url = serviceName + parameters;
	var http_request = createXHR();
	// alert("request created");
	http_request.onreadystatechange = function() {
		// alert("request status"+http_request.status);
		if (http_request.readyState == 4) {
			if (http_request.status == 200) {
				// alert("now calling request");
				result = http_request.responseText;
				loadTree(result);

			} else {
				alert('There was a problem with the request.');
			}
		}
	}
	http_request.open('GET', url, true);
	http_request.send(null);

}

/**
 * 
 */
function renderClarificationsTable(tBody, data) {

	var rowId=1;
	for (i = 0; i < (data.textResponse.clarifications.length); i++) {
		var description = " I struggle with '"
			+ data.textResponse.clarifications[i].stringToClarify + "'. ";
	        description = description + "Is '"
			    + data.textResponse.clarifications[i].stringToClarify
			    + "' related to: ";
	var anotherTr = document.createElement('TR');
	var tdRowIdHeader = document.createElement('TD');
	var td11 = document.createElement('TD');
	var functionTd = document.createElement('TD');
	var td21 = document.createElement('TD');
	anotherTr.style.backgroundColor = '#D3DAED';
	//td11.style.backgroundColor = 'red';
	tBody.appendChild(anotherTr);
	anotherTr.appendChild(tdRowIdHeader);
	anotherTr.appendChild(td11);
	anotherTr.appendChild(functionTd);
	anotherTr.appendChild(td21);
	tdRowIdHeader.appendChild(document.createTextNode(""));
	td11.appendChild(document.createTextNode(description));
	td21.appendChild(document.createTextNode(""));
		//alert("first stop:"+data.textResponse.clarifications.length);
		for (j = 0; j < data.textResponse.clarifications[i].clarificationOptions.length; j++) {
				// add header for that interpretation description

			var tr = document.createElement('TR');
			
			var tdRowId = document.createElement('TD');
			var td0 = document.createElement('TD');
			var td1 = document.createElement('TD');
			var td2 = document.createElement('TD');
			tr.style.backgroundColor = j % 2 ? '#FFF' : '#EFF4FF';
			tBody.appendChild(tr);
		
			tr.appendChild(tdRowId);
			tr.appendChild(td0);
			tr.appendChild(td1);
			tr.appendChild(td2);
			tdRowId.appendChild(document.createTextNode(rowId));
			rowId=rowId+1;
			
			var voteId=data.textResponse.clarifications[i].clarificationOptions[j].id;
			var option = data.textResponse.clarifications[i].clarificationOptions[j].option
					+ " ( "
					+ data.textResponse.clarifications[i].clarificationOptions[j].vote + ")";
            var functionValue=data.textResponse.clarifications[i].clarificationOptions[j].function;

            td0.appendChild(document.createTextNode(functionValue));
			td1.appendChild(document.createTextNode(option));
			
			
			
			var valueForRadio = voteId;
			try {
				rdo = document
						.createElement('<input type="checkbox" name="voteId" value="' + valueForRadio + '" />');
				rdo.onclick = function() {
					//var obj = document.getElementById('myform');
					//refine();
				}
			} catch (err) {
				rdo = document.createElement('input');
				rdo.setAttribute('type', 'checkbox');
				rdo.setAttribute('name', 'voteId');
				rdo.setAttribute('value', valueForRadio);
				rdo.onclick = function() {
					//var obj = document.getElementById('myform');
					//refine();
				}
			}
			td2.appendChild(rdo);
			
			//var trForButton= document.createElement('TR');
			//tBody.appendChild(trForButton);
			var tdForButton=document.createElement('TD');
			//trForButton.appendChild(tdForButton);
			tr.appendChild(tdForButton);
			var buttonElementString='<input type="button" value="Go" name="refButton"/>';
			var refButton;
			try {
				refButton = document.createElement(buttonElementString)
				refButton.onclick = function() {
								refine();
				}
			} catch (err) {
				refButton = document.createElement('input');
				refButton.setAttribute('type', 'button');
				refButton.setAttribute('name', 'refButton');
				refButton.setAttribute('value', 'Go');
				refButton.onclick = function() {
				
					refine();
				}
			
				tdForButton.appendChild(refButton);
		}// iterating through j

	}// iterating through i
	

	}
	
	
}
function showResult(fname) {
	// alert(" before creating request");
	var xhr = createXHR();
	xhr.open("GET", fname, true);
	// alert("request created");
	xhr.onreadystatechange = function() {
		// alert(xhr.status);
		if (xhr.readyState == 4) {
			if (xhr.status != 404) {
				//alert(xhr.responseText);

				} else {
				document.getElementById("zone").innerHTML = fname
						+ " not found";
			}// readystate=404
			
		}// readystate=4
	}// on ready function
	xhr.send(null);
}
function renderTable(fname) {
	// alert(" before creating request");
	var xhr = createXHR();
	xhr.open("GET", fname, true);
	// alert("request created");
	xhr.onreadystatechange = function() {
		// alert(xhr.status);
		if (xhr.readyState == 4) {
			if (xhr.status != 404) {
				//alert(xhr.responseText);
				var data = eval("(" + xhr.responseText + ")");
				
				//alert("data:"+data);
				//alert("xhr.responseText:"+xhr.responseText);
				if (data[0].textResponse == null || data[0].textResponse.clarifications == null) {
					//alert("now loading results");
					//alert("data.table:"+data.table);
					if (data[0].table !=null){
						renderResultsTable(data[0]);
						//alert("data.table != null!"+data.table);
				}
					if (data[0].graph !=null){
						//hide the canvas if the number of nodes is more than 200
						clearTheClarificationsTable();
						if (data[0].graph.length>200){
							document.getElementById("resultContainer").style.display = "none";
							document.getElementById("zone").innerHTML="Results are shown in the table only as there were too many.";
							//hide it
						} else {
							document.getElementById("resultContainer").style.display = "block";
//							right-container
						    loadTree(data[0].graph);
						}
					}
				//loadTree(xhr.responseText);
				} else {
				//alert("now rendering dialog");;
                    clearTheClarificationsTable();
                    var tBody = document.createElement('tbody');
					if (data[0].textResponse.clarifications.length > 0) {
						//alert("data in render table: "+data);
						renderClarificationsTable(tBody, data[0]);
					} else {
						var noInterpretationsMessage = "<h4>We could not find any results for your query. Maybe you want to refine it?</h4>";
						document.getElementById("zone").innerHTML = noInterpretationsMessage;
					}
					
				var table = document.getElementById('interpretationsTable');
				
				table.appendChild(tBody);

				}//dialog
				} else {
				document.getElementById("zone").innerHTML = fname
						+ " not found";
			}// readystate=404
			
		}// readystate=4
	}// on ready function
	xhr.send(null);
}

/**
 * clear the table with the dialog elements
 */
function clearTheResultsTable(){
	//alert("cleaning...");
	var table = document.getElementById('resultsTable');
    var children;
	if (table!=null){
		children=table.childNodes;
	}
	//alert("inside clear table 2");
	for ( var i = children.length - 1; i >= 0; i--) {
		table.removeChild(table.childNodes[i]);
	}
	//alert("finished cleaning...");

}


/**
 * clear the table with the dialog elements
 */
function clearTheClarificationsTable(){
	var table = document.getElementById('interpretationsTable');
	var tableHeader = document.getElementById('fixedHeader');
	//alert("inside clear table 1");
    var children;
    var headChildren;
	if (table!=null){
		children=table.childNodes;
	}
	//alert("inside clear table 2");
	for ( var i = children.length - 1; i >= 0; i--) {
		table.removeChild(table.childNodes[i]);
	}
	//alert("inside clear table 3");
	if (tableHeader!=null){
		headChildren=tableHeader.childNodes;
	}
	//alert("inside clear table 4");
	for ( var i = headChildren.length - 1; i >= 0; i--) {
		tableHeader.removeChild(tableHeader.childNodes[i]);
	}
	//alert("inside clear table 5");
	var zoneEl=	document.getElementById("zone")
	if (zoneEl!=null)
	zoneEl.innerHTML = "";
}

/**
 * clear all elements of the page
 */
function cleanThePage(){
	var clabel=	document.getElementById('mycanvas-label');
	if (clabel!=null)
	clabel.innerHTML = '';
	//alert("right-container");
	var rc=document.getElementById('right-container');
	if (rc!=null)
	rc.innerHTML = '';
	
	var mcanvas=document.getElementById('mycanvas');
	if (mcanvas!=null){
	//somehow delete canvas?
		//mcanvas.innerHTML='';// did not work
	}
	//now clean the dialog if neccessary
	clearTheClarificationsTable();
	clearTheResultsTable();
}
/**
 * 
 */
function getSparql() {
	//clear the graph and the right container
	//alert("about to call cleanThePage");
	cleanThePage();
	//alert("finished call cleanThePage");
	//alert("processing request...");
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'getSparql';
	var url = serviceName + getParameters(obj);
	// alert(url);
	showResult(url);
	}

/**
 * 
 */
function ask() {
	//clear the graph and the right container
	//alert("about to call cleanThePage");
	cleanThePage();
	//alert("finished call cleanThePage");
	//alert("processing request...");
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'ask';
	var url = serviceName + getParameters(obj);
	// alert(url);
	renderTable(url);
	}

/**
 * 
 */
function askAuto() {
	//clear the graph and the right container
	//alert("about to call cleanThePage");
	cleanThePage();
	//alert("finished call cleanThePage");
	//alert("processing request...");
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'askNoDialog';
	var url = serviceName + getParameters(obj);
	// alert(url);
	renderTable(url);
	}

/**
 * 
 */
function askForceDialog() {
	//clear the graph and the right container
	//alert("about to call cleanThePage");
	cleanThePage();
	//alert("finished call cleanThePage");
	//alert("processing request...");
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'askForceDialog';
	var url = serviceName + getParameters(obj);
	// alert(url);
	renderTable(url);
	}

function askNoFilter() {
	//clear the graph and the right container
	//alert("about to call cleanThePage");
	cleanThePage();
	//alert("finished call cleanThePage");
	//alert("processing request...");
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'askNoFilter';
	var url = serviceName + getParameters(obj);
	// alert(url);
	renderTable(url);
	}

/**
 * 
 */
function refine() {
	var obj = document.getElementById('myform');
	var serviceName = 'service/' + 'refine';
	var url = serviceName + getParameters(obj);
	// alert(url);
	renderTable(url);
	}




function renderResultsTable(data) {
	var tBody = document.createElement('tbody');
	var table = document.getElementById('resultsTable');
	table.appendChild(tBody);
	//alert("data.table.length:"+data.table.length)
	for (i = 0; i < (data.table.length); i++) {
		var anotherTr = document.createElement('TR');
		var tdRowIdHeader = document.createElement('TD');
		anotherTr.style.backgroundColor = i % 2 ? 'black' : 'grey';
		tBody.appendChild(anotherTr);
		anotherTr.appendChild(tdRowIdHeader);
		tdRowIdHeader.appendChild(document.createTextNode(i));
		for (j=0;j<(data.table[i].length); j++){
			var label = data.table[i][j].label;
			var uri = data.table[i][j].uri;
			
			if (i>0){
				var anchorTag=anchorTag=document.createElement('a');
			    anchorTag.appendChild(document.createTextNode(label));
                anchorTag.href = uri;
		        var td11 = document.createElement('TD');
		        anotherTr.appendChild(td11);
		        td11.appendChild(anchorTag);	
		    } else {
			    var td11 = document.createElement('TD');
			    anotherTr.appendChild(td11);
			    td11.appendChild(document.createTextNode(label));
			}
		}
	}
}