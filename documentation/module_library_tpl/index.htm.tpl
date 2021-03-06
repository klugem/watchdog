
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Watchdog's module library</title>
	<link rel="icon" href="gfx/watchdog.ico">
	<!-- CSS FILES -->
	<link rel="stylesheet" type="text/css" href="css/uikit.min.css">
	<link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.10.19/css/jquery.dataTables.css">
	<link rel="stylesheet" type="text/css" href="css/modules.css">
</head>
<body>
	<div class="uk-offcanvas-content">
		<!--HEADER-->
		<header id="site-head">
			<div class="uk-container uk-container-expand">
				<div class="uk-grid uk-grid-medium uk-flex uk-flex-middle" data-uk-grid>
					<div class="uk-width-auto">
						<img class="uk-logo" src="gfx/watchdog.png" alt="" style="width: 64px" />
					</div>
					<div class="uk-width-expand">
						<div class="uk-inline uk-width-1.1">
							<span class="uk-form-icon uk-form-icon" data-uk-icon="icon: search"></span>
							<input class="uk-input uk-width-auto search-fld" type="search" placeholder="Search..." id="search-input" oninput="moduleFilter(true)">
							<span class="uk-width-auto"><input class="uk-checkbox" type="checkbox" id="search-name" checked onchange="moduleFilter(false)"/> title</span>
							<span class="uk-width-auto"><input class="uk-checkbox" type="checkbox" id="search-author" checked onchange="moduleFilter(false)"/> author</span>
							<span class="uk-width-auto"><input class="uk-checkbox" type="checkbox" id="search-category" checked onchange="moduleFilter(false)"/> category</span>
							<span class="uk-width-auto"><input class="uk-checkbox" type="checkbox" id="search-description" checked onchange="moduleFilter(false)"/> description</span>
						</div>
					</div>
					<div class="uk-width-auto">
						<ul class="uk-subnav" data-uk-margin>
							<li class="uk-visible@s"><a href="#">About</a></li>
						</ul>
					</div>
				</div>
			</div>
		</header>
		<!--/HEADER-->
<div class="spacer"></div>
<!--MODULE WRAPPER-->
<section id="filter-modules" class="uk-section uk-section-small uk-section-default uk-padding-remove-bottom" data-uk-filter="target: .js-filter">
	<div class="uk-container uk-container-expand uk-margin-large-bottom">
	{@FILTER_REPLACE_TEMPLATE@}

	<div class="uk-grid uk-grid-medium uk-child-width-1-2@s uk-child-width-1-3@m uk-child-width-1-4@l  uk-child-width-1-6@xl uk-grid-match js-filter" data-uk-grid="masonry: true" data-uk-sortable="handle: .drag-icon">
		{@MODULE_REPLACE_TEMPLATE@}

	</div>
</div>
</section>
<!--/MODULE WRAPPER-->
<!--FOOTER-->
<footer id="site-foot" class="uk-section uk-section-secondary uk-section-xsmall">
	<div class="uk-container uk-text-small uk-text-center">
		<a href="https://github.com/klugem/watchdog" title="Visit Watchdog on Github" target="_blank" data-uk-tooltip>Watchdog's module reference book</a> | Built with <a href="http://getuikit.com" title="Visit UIkit 3 site" target="_blank" data-uk-tooltip><span data-uk-icon="uikit"></span></a> and <a href="https://github.com/zzseba78/Kick-Off" title="Visit Kick-Off site" target="_blank" data-uk-tooltip>Kick-Off</a>
	</div>
</footer>
<!--/FOOTER-->


<!-- JS FILES -->
<script src="js/uikit.min.js"></script>
<script src="js/uikit-icons.min.js"></script>
<script src="js/jquery.min.js"></script>
<script src="js/meta.json"></script>
<script src="js/moduleFilter.js"></script>
<script src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
</body>
</html>
