
<div class="cat{@CAT_ID@}-card module-parent" style="transform: translateY(0px);" meta-author="{@META_AUTHOR@}" meta-category="{@META_CATEGORY@}" meta-update="{@META_AGE@}" meta-search="true" meta-id="{@META_ID@}">
	<!-- overview card -->
	<div class="uk-card uk-card-small uk-card-default">
		<div class="uk-card-header">
			<div class="uk-grid uk-grid-small uk-text-small" data-uk-grid>
				<div class="uk-width-expand">
					<span class="cat-txt">{@CATEGORY@}</span>
				</div>
				<div class="uk-width-auto uk-text-right uk-text-muted">
					<span data-uk-icon="icon:history; ratio: 1"></span> {@AGE@}
				</div>
			</div>
		</div>
		<div class="uk-card-body">
			<h5 class="uk-margin-small-bottom uk-margin-remove-adjacent uk-text-bold"><a class="uk-link-heading" href="#module-full{@META_ID@}" onclick="initFullModule({@META_ID@}, {@VERSION@})" uk-toggle>{@MOD_NAME@}</a></h5>
			<p class="uk-text-small uk-text-muted versionSwitchDescription">{@DESC@}</p>
		</div>
		<div class="uk-card-footer">
			<div class="uk-grid uk-grid-small uk-grid-divider uk-flex uk-flex-middle" data-uk-grid>
				<div class="uk-width-expand uk-text-small uk-text-nowrap">{@AUTHOR@}</div>
				<div class="uk-width-auto">
					<a href="{@GITHUB@}" data-uk-tooltip="title: Github" target="_blank" class="uk-icon-link" data-uk-icon="icon:github; ratio: 1"></a><!--GIT_LINK-->
					<a href="{@URL@}" data-uk-tooltip="title: Website" target="_blank" class="uk-icon-link" data-uk-icon="icon:link; ratio: 1"></a><!--URL_LINK-->
					<a href="https://www.ncbi.nlm.nih.gov/pubmed/{@PMID@}" data-uk-tooltip="title: PMID" target="_blank" class="uk-icon-link" data-uk-icon="icon:quote-right; ratio: 1"></a><!--PMID_LINK-->
				</div>
				<!--<div class="uk-width-auto uk-text-right">
					<span data-uk-icon="icon:tag; ratio: 1" data-uk-tooltip="version"></span> {@VERSION@}
				</div>-->
			</div>
		</div>
	</div>
	<!-- full description -->
	<div id="module-full{@META_ID@}" class="uk-modal-full" meta-id="{@META_ID@}" uk-modal>
		<div class="uk-modal-dialog">
			<button class="uk-modal-close-full uk-close-large" type="button" uk-close></button>
			<div class="uk-grid-collapse uk-flex-middle" uk-grid>
				<div class="uk-background-cover" uk-height-viewport></div>
				<div class="uk-padding-large uk-width-auto">
					<h1>{@MOD_NAME@}</h1>
					<h3><i>by</i> {@AUTHOR@} - version {@VERSION@}
					<div class="uk-inline mayhide" meta-hide="{@VERSION_LINKS_HIDE@}">
				    <div class="uk-text-small">version
							<a class="uk-link-reset uk-badge" style="margin-left: 10px;" href="#" onclick="setVersion({@META_ID@}, {@VERSION_ID@})">{@VERSION_ID@}</a> <!--VERSION_LINK_TEMPLATE-->
							{@VERSION_LINKS@}
						</div>
					</div>
					</h3>
					<div class="mayhide" meta-hide="{@DEPENDENCIES_HIDE@}">
					<p id="description-switch{@META_ID@}">{@DESC@}</p>
					<h3 class="uk-h3"><a href="#auto-and-expand">Dependencies</a></h3>
					<ul class="uk-list uk-list-bullet">
						<li class="versionMetaFilter" meta-minVersion="{@MIN_VER@}" meta-maxVersion="{@MAX_VER@}">{@SINGLE_DEPENDENCY@}</li> <!--DETAIL_DEPENDENCY_TEMPLATE-->
						{@DEPENDENCIES@}
					</ul><br /><br />
				</div>
					<div class="mayhide" meta-hide="{@PARAMETER_HIDE@}">
					<h3 class="uk-h3"><a href="#auto-and-expand">Parameter</a></h3>
					<table width="100%" class="uk-table uk-table-justify uk-table-striped uk-table-hover uk-width-1-1" id="parameterTable{@META_ID@}" style="width:100%">
						<thead>
							<tr>
								<th>name</th>
								<th>type</th>
								<th>restrictions</th>
								<th>default value</th>
								<th>occurrence</th>
								<th>description</th>
								<th>minV</th>
								<th>maxV</th>
							</tr>
						</thead>
						<tr><td>{@PARAM_NAME@}</td><td>{@PARAM_TYPE@}</td><td>{@PARAM_RESTRICTION@}</td><td>{@PARAM_DEFAULT@}</td><td>{@PARAM_OCCURENCE@}</td><td>{@PARAM_DESCRIPTION@}</td><td>{@MIN_VER@}</td><td>{@MAX_VER@}</td></tr> <!--DETAIL_PARAM_TEMPLATE-->
						{@PARAMETER@}
					</table><br /><br />
				</div>
					<div class="mayhide" meta-hide="{@RETURN_VALUES_HIDE@}">
					<h3 class="uk-h3"><a href="#auto-and-expand">Return values</a></h3>
					<table class="uk-table uk-table-justify uk-table-striped uk-table-hover uk-width-1-1" id="returnTable{@META_ID@}" style="width:100%">
						<thead>
							<tr>
								<th>name</th>
								<th>type</th>
								<th>description</th>
								<th>minV</th>
								<th>maxV</th>
							</tr>
						</thead>
						<tr class="minVersion-{@MIN_VER@} maxVersion-{@MAX_VER@}"><td>{@RETURN_NAME@}</td><td>{@RETURN_TYPE@}</td><td>{@RETURN_DESCRIPTION@}</td><td>{@MIN_VER@}</td><td>{@MAX_VER@}</td></tr> <!--DETAIL_RETURN_TEMPLATE-->
						{@RETURN_VALUES@}
					</table><br /><br />
					</div>
					<div class="mayhide" meta-hide="{@CITATION_HIDE@}">
					<h3 class="uk-h3"><a href="#auto-and-expand">Citation info</a></h3>
					<p>{@PAPER_DESC@}</p>
					<p>Pubmed references:
						<a href="https://www.ncbi.nlm.nih.gov/pubmed/{@SINGLE_PMID@}" data-uk-tooltip="title: PMID" target="_blank">{@SINGLE_PMID@}</a>, <!--DETAIL_CITE_TEMPLATE-->
						{@PMID_LIST@}
					</p>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
