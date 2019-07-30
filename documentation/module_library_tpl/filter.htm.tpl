<div class="uk-grid-small uk-grid-divider uk-child-width-auto uk-grid" uk-grid="">
	<div>
		<ul id="refresh-search" class="uk-subnav uk-subnavCategory uk-subnav-pill" uk-margin>
			<li class="uk-active" data-uk-filter-control="group: category"><a href="#" id="filter-category-link0">All Categories</a></li>
			<li data-uk-filter-control="filter: [meta-category*='{@VALUE@}']; group: category"><a href="#" id="filter-category-link{@LINK_COUNTER@}">{@VALUE_DISPLAY@}</a></li><!--FILTER_BUTTON_TEMPLATE_CATEGORY-->
		</ul>
	</div>
	<div>
		<ul class="uk-subnav uk-subnav-pill" uk-margin>
			<li class="uk-active" data-uk-filter-control="group: author"><a href="#" id="filter-author-link0">All Authors</a></li>
			<li data-uk-filter-control="filter: [meta-author*='{@VALUE@}']; group: author"><a href="#" id="filter-author-link{@LINK_COUNTER@}">{@VALUE_DISPLAY@}</a></li><!--FILTER_BUTTON_TEMPLATE_AUTHOR-->
		</ul>
	</div>
	<div>
		<ul class="uk-subnav uk-subnav-pill" uk-margin>
			<li class="uk-active" data-uk-filter-control="group: age"><a href="#" id="filter-age-link0">All last update</a></li>
			<li data-uk-filter-control="filter: [meta-age*='{@VALUE@}']; group: age"><a href="#" id="filter-age-link{@LINK_COUNTER@}">{@VALUE_DISPLAY@}</a></li><!--FILTER_BUTTON_TEMPLATE_AGE-->
		</ul>
	</div>
	<!-- custom meta filter tag for text-search filter -->
	<span class="uk-active" data-uk-filter-control="filter: [meta-search=true]; group: search"></span>
</div>
