function add() {
	var i = parseInt(document.getElementById("counter").innerHTML);
	var type="flag"
	var isFlag = true;
	if(confirm("Do you want to add a parameter with an argument?") == true) {
		isFlag = false;
		type="parameter"
	}
	var nameParam = prompt("Please enter a name for the " + type + ": ", "");
	if(nameParam != null) {
		var start = '<td><img src="/png/delete.png" onclick="del(\''+i+'\')" title="delete parameter"/></td><td>'+nameParam+'</td>'
		var middle = '<td><input type="checkbox" name="settings_'+nameParam+'" checked="true" value="#@CHECK|CHECK@#"></td>';
		if(isFlag == false) {
			var valueParam = prompt("Please enter the value for the parameter: ", "");
			if(valueParam != null) {
				middle = '<td><input type="text" name="settings_'+nameParam+'" value="'+valueParam+'"></td>';
			}
			else {
				return;
			}
		}
		var el = document.createElement('tr');
		el.setAttribute("id", i);
		el.innerHTML = start + middle
		document.getElementById("paramTable").appendChild(el);
		i = i + 1;
		document.getElementById("counter").innerHTML = i;
	}
}

function del(i) {
	var e = document.getElementById(i);
	e.outerHTML = "";
	delete e;
}
