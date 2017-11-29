
function unhide() {
	var ele = document.getElementsByClassName('button');
	for (var i = 0; i < ele.length; ++i) {
	    var item = ele[i];  
	    item.style.display = 'block';
	}
}
