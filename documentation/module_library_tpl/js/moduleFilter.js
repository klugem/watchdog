// true, if input is from text search
var isTextSearchUsed = false;

// parse data to search just once
var metaOrg = jQuery.parseJSON(meta_info);
meta = []
now = new Date() / 1000
for (var i = 0; i < metaOrg.length; i++) {
	entry = metaOrg[i]
	ago = now - (Date.parse(entry.updated) / 1000)
	age = "other"
	if(ago <= 24*3600*3)
		age = "3 days"
	else if(ago <= 24*3600*7)
		age = "1 week"
	else if(ago <= 24*3600*21)
		age = "3 weeks"
	entry.age = age.toUpperCase()
	meta[entry.iID] = entry
}



// functions that are executed when page is loaded
$(document).ready(function() {
	// custom filtering function
	$.fn.dataTable.ext.search.push(
		function(settings, data, dataIndex) {
			var id = settings.sTableId
			if (id.startsWith("returnTable") || id.startsWith("parameterTable")) {
				var baseIndex = 6;
				var iID;
				if (id.startsWith("returnTable")) {
					var baseIndex = 3;
					iID = id.replace("returnTable", "")
				} else {
					iID = id.replace("parameterTable", "")
				}

				var info = meta[iID]
				var cVer = info.cVer
				if (data[baseIndex] == 0 || (data[baseIndex] <= cVer && data[baseIndex + 1] >= cVer)) {
					return true
				} else {
					return false
				}
			} else {
				return true
			}
		}
	);

	// init age
	for (var i = 0; i < meta.length; i++) {
		var info = meta[i]
		var element = $("div.module-parent[meta-id='" + info.iID + "']")
		// ensure that module was found
		if (typeof element !== "undefined") {
			element.attr("meta-age", '§'+info.age+'§')
		}
	}

	// update state of filter links after click
	var filterDIV = $('#filter-modules')
	filterDIV.on('afterFilter', function() { updateFilterLinks() });

	// update filter buttons
	moduleFilter(false);
});

// is called when the user clicks on a module --> full module description
function initFullModule(iID, version) {
  // hide unused stuff
  $("div[meta-id='" + iID + "'] .mayhide[meta-hide='true']").each(function() {$(this).hide()})

  // filter versioned elements
  setVersion(iID, version)
}

// changes the contents of the full page depending on the version
function setVersion(iID, version) {
	// change description
	var info = meta[iID]
	info.cVer = version // change version
	var description = info["description"]
	for (var i = 0; i < description.length; i++) {
		var desc = description[i]
		if (desc["MIN_VER"] <= version && desc["MAX_VER"] >= version) {
			$("#description-switch" + iID).text(desc["VALUE"])
			break
		}
	}

  // filter elements based on meta-version attributes
  $("div[meta-id='" + iID + "'] .versionMetaFilter").each(function() {
    var mi = $(this).attr("meta-minVersion")
    var ma = $(this).attr("meta-maxVersion")
    var info = meta[iID]
    var cVer = info.cVer
    if (mi <= cVer && ma >= cVer) {
      $(this).show()
    }
    else {
      $(this).hide(
      )
    }
  });

	// hide rows in table
  if(info.wasInit === undefined) {
    info.wasInit = true

    // init the JS tables
    $("#parameterTable" + iID).DataTable({
      "paging": false,
      "info": false,
      "language": {
        "infoFiltered": ""
      },
      "columnDefs": [{
        "targets": [6, 7],
        "visible": false,
      }]
    });
    $("#returnTable" + iID).DataTable({
      "paging": false,
      "info": false,
      "language": {
        "infoFiltered": ""
      },
      "columnDefs": [{
        "targets": [3, 4],
        "visible": false,
      }]
    });
  }
  else {
  	$("#parameterTable" + iID).DataTable().draw();
    $("#returnTable" + iID).DataTable().draw();
  }
}

// updates the counter function for the categories
function updateCounter(counter, cat, value) {
	for (var i = 0; i < value.length; i++) {
		var v = value[i].toUpperCase()
		if (counter[cat][v] === undefined)
			counter[cat][v] = 1
		else
			counter[cat][v] ++
	}
}

function sortCounter(counter, cat) {
	var obj = counter[cat]
	var keys = Object.keys(obj)
	keys.sort(function(a, b) {
		return obj[b] - obj[a]
	});
	return keys
}


function updateFilterButtons(counter, top, idBase, cat, number) {
	active = undefined
	// get active cat's
	for (var i = 0; i <= number; i++) {
		var ele = $("#" + idBase + i)
		var lie = ele.parent()
		if(lie.hasClass("uk-active")) {
			active = ele.text().replace(/ \([0-9]+\)/, "")
		}
	}

	// if no text search, blur active element as otherwise wrong classes might be marked as active
	if(!isTextSearchUsed)
		document.activeElement.blur()
	for (var i = 1; i <= number; i++) {
		var ele = $("#" + idBase + i)
		if (top.length >= i) {
			var value = top[(i - 1)]
			var count = counter[cat][value]

			ele.text(value + " (" + count + ")")

			// update meta
			var lie = ele.parent()
			lie.removeClass("uk-active")
			var metaText = lie.attr("data-uk-filter-control").replace(/§.+§/, '§'+value+'§')
			lie.attr("data-uk-filter-control", metaText)

			// trigger as order might be changed
			if(value === active) {
				lie.addClass("uk-active")
			}
			ele.show()
		} else {
			ele.text("")
			ele.hide()
		}	
	}
}

// tests if any element in the array matches the regex
function searchArray(array, regex) {
	for(var i = 0; i < array.length; i++) {
		if(array[i].search(regex) != -1) {
			return true
		}
	}
	return false
}

function updateFilterLinks() {
	var counter = {}
	counter["author"] = {}
	counter["category"] = {}
	counter["age"] = {}
	// check, which elements are visible
	jQuery.each(meta, function(key, obj) {
		var element = $("div.module-parent[meta-id='" + obj.iID + "']")

		// ensure that module was found
		if (typeof element !== "undefined") {
			if(element.attr('meta-search') && element.is(':visible')) {
				updateCounter(counter, "author", obj.author)
				updateCounter(counter, "category", obj.category)
				updateCounter(counter, "age", [obj.age])
			}
		}
	});

	// sort & update the filter buttons
	topA = sortCounter(counter, "author")
	topC = sortCounter(counter, "category")
	topU = sortCounter(counter, "age")

	updateFilterButtons(counter, topA, "filter-author-link", "author", 5)
	updateFilterButtons(counter, topC, "filter-category-link", "category", 5)
	updateFilterButtons(counter, topU, "filter-age-link", "age", 3)
}


// function that must be called when search results should be updated
function moduleFilter(isTextEdited) {
	if(isTextEdited)
		isTextSearchUsed = true
	else
		isTextSearchUsed = false

	// check which field to search
	var name = $("#search-name").is(':checked')
	var author = $("#search-author").is(':checked')
	var category = $("#search-category").is(':checked')
	var description = $("#search-description").is(':checked')

	// regex to search for
	var searchInput = $("#search-input").val()
	var regex = new RegExp(".*"+ searchInput + ".*", "i")

	// apply search on all modules
  	jQuery.each(meta, function(key, obj) {
		var element = $("div.module-parent[meta-id='" + obj.iID + "']")

		// ensure that module was found
		if (typeof element !== "undefined") {
			var show = false
			if (name && obj.name.search(regex) != -1) 
				show = true;
			else if(author && searchArray(obj.author, regex))
				show = true
			else if(category && searchArray(obj.category, regex))
				show = true
			else if(description && searchArray(obj.description_search, regex))
				show = true

			// show element or hide it
			if (show) {
				element.attr("meta-search", "true")
			} else {
				element.attr("meta-search", "false")
			}
		}
	});

	// simulate real search --> update meta-search
	var target = $("#refresh-search > li.uk-active").click()
}
