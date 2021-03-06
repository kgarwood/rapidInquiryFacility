---
layout: default
title: The use of Angular.js in RIF Front End, April 2019
---

1. Contents
{:toc}

# Introduction

Assumes you are familiar with Angular nomenclature, so you know a partial from a directive.

* In this document RIF will be used to refer to the front-end of the RIF;
* Uses the Angular 1.x JavaScript framework. There is no JQuery at all;
* Tested extensively on Firefox, Chrome, Edge. Will work on Internet Explorer, Opera and most 
  Firefox/Chromium browser derivatives;
* See the GitHub wiki of how to open in NetBeans;
* See the [RIF Web Application and Middleware Installation](../Installation/rifWebApplication) on how to set 
  up Tomcat, rifServices.war etc;
* All libraries need to be hard-wired as the RIF has to be standalone and not via CDNs (no internet 
  connection if running on a private network);
* There will be no map tiles on the private network (unless we cache them as planned). There is code for local 
  caching on the TopoJSON GridLayer, however it is very dependent on browser permissions and not in use;
* Various libraries are used, see index.html. Mainly, the Bootstrap library is used for modal dialogues, 
  Leaflet for map containers, D3 for graphs, ui-grid and ui-layout;
* There are no unit tests and the code has not been JS-Linted. Did not have the time or resources.

# General Layout of the RIF files 

## Directory Structure

The file are located in *rifWebApplication\src\main\resources*. From this file root, the RIF has the following 
directory structure:

| Directory name | Description                                                                                                                                                                                                                         |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| backend        | Functionality to deal with the database via the middleware                                                                                                                                                                          |
| css            | RIF specific css                                                                                                                                                                                                                    |
| dashboards     | Functionality to deal with the main RIF tab states. e.g. viewer, login. Core dashboard modals, divided into export, login, mapping, submission and viewer and then sub divided into: controllers, directives, partials and services |
| images         | Images used by the RIF, divided into: colorBrewer, glyphicon and trees                                                                                                                                                              |
| libs           | All third-party libraries - Hardwired in, do not use remote sources. Generally browserified code is is standalone and other libraries including RIF modified ones are in the root (libs)                                            |
| modules        | Contains the definition of the RIF angular module                                                                                                                                                                                   |
| utils          | Functionality shared over more than one part of the RIF, divided into controllers, directives, partials and services                                                                                                                |

In the root there is also the index.html. Here all the paths to third-party libraries are given (i.e the local 
libs directory), all RIF specific JavaScript files, css etc. The placeholder for the Angular content is also 
defined within a div in the html body - "data-ui-view" and also the directive for the notifications bar. 
Dashboards is further split into five directories, these represent the tabs seen in the RIF GUI, also in the 
RIF main module, these are the five states defined in the "$stateProvider" (see below). The utils directory 
has functionality that is shared by more than one of these dashboards, this is mostly to do with mapping of 
results.

Contained within these directories are separate folders for Angular controllers, directives, partials and services for the relevant part of the RIF.

## Naming Convention

All RIF specific files use the same convention: *rif(type)-(usedby)-(description).(extension)*

For example:

* **rifc-dmap.main.js**: *rif(controller)-(used by disease mapping dashboard)-(the main controller).(js file)*

File types can be:

| Suffix | Description	              | Extension |
|--------|----------------------------|-----------|
| c	 	 | Angular controller         | .js       |
| d		 | Angular directive	      | .js       |
| m		 | Angular module     	      | .js       |
| s		 | Angular service or factory | .js       |
| p		 | HTML partial               | .html     |
| x		 | CSS                        | .css      |

*rif[c/p/d/s]-&lt;specific dashboard or utility&gt;-&lt;name&gt;* E.g:

* rifp-dsub-main.html: partial for the main submission screen (with the four trees)
* rifc-dsub-main.js: controller for the main submission screen

They will be found in dashboards/submission.

The abbreviations [c/p/d/s] are:

* **c**: controller;
* **d**: directive;
* **p**: partial;
* **s**: service.

The specific dashboard or utility is:

* **dsub**: study submission dashboard in in *dashboards/submission*;
* **expt**: study export dashboard;
* **login**: login popup modal;
* **dmap**: dual map mapping dashboard;
* **view**: map and data viewer dashboard;
* **util**: utilities;

# Libraries

| Library                                                                                 | Files                                                                          |
|-----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| [AngularJS 1.5.8 ](https://angularjs.org/)                                              | angular.min.js                                                                 |
| - [CSS-based Animations](https://docs.angularjs.org/api/ngAnimate)                      | angular-animate.min.js                                                         |
| - [Sanitize HTML.](https://docs.angularjs.org/api/ngSanitize)                           | angular-sanitize.min.js                                                        |
| - [ARIA attributes](https://docs.angularjs.org/api/ngAria)                              | angular-aria.min.js                                                            |
| - [Vslidation messages](https://docs.angularjs.org/api/ngMessages)                      | angular-messages.min.js                                                        |
| - [Simple logger](https://github.com/nmccready/angular-simple-logger)                   | angular-simple-logger.js                                                       |
| - [Material Design components](https://material.angular.io/)                            | angular-material.min.js, libs/standalone/angular-material.min.css              |
| - [UI Bootstrap modal windows](https://angular-ui.github.io/bootstrap/)                 | ui-bootstrap-tpls-2.3.0.min.js, bootstrap.min.css                              |
| - [UI Router state machine](https://github.com/angular-ui/ui-router/wiki)               | angular-ui-router.min.js                                                       |
| - [UI Grid](http://ui-grid.info/)                                                       | ui-grid.min.js, ui-grid.min.css                                                |
| - [UI Layout](https://github.com/angular-ui/ui-layout)                                  | uui-layout.css, ui-layout.js                                                   |
| - [Notifications bar](https://github.com/alexbeletsky/ng-notifications-bar)             | ngNotificationsBar.css, ngNotificationsBar.min.js                              |
| - [Pattern restriction](https://github.com/AlphaGit/ng-pattern-restrict)                | ng-pattern-restrict.min.js                                                     |
| - [Logging](https://github.com/nmccready/angular-simple-logger)                         | angular-simple-logger.js                                                       |
| [Leaflet 1.0.3](https://leafletjs.com/)                                                 | leaflet.css, leaflet.js                                                        |
| - [Draw](https://github.com/Leaflet/Leaflet.draw)                                       | leaflet.draw.css, leaflet.draw.js                                              |
| - [Fullscreen](https://github.com/Leaflet/Leaflet.fullscreen)                           | Leaflet.fullscreen.min.js, leaflet.fullscreen.css                              |
| - [Map sync](https://github.com/jieter/Leaflet.Sync)                                    | L.Map.Sync.js                                                                  |
| - [Map spinner](https://github.com/makinacorpus/Leaflet.Spin)                           | spin.min.js, spin.js, spin.css, leaflet.spin.min.js                            |
| - [Geosearch](https://github.com/smeijer/leaflet-geosearch)                             | l.control.geosearch.js, l.geosearch.provider.openstreetmap.js, l.geosearch.css |
| - [Opacity slider](https://github.com/Eclipse1979/leaflet-slider)                       | leaflet-slider.js, leaflet-slider.css                                          |
| - [Shapefile support](https://github.com/calvinmetcalf/leaflet.shapefile)               | leaflet.shpfile.js, shp.min.js                                                 |
| - [Image export](https://github.com/mapbox/leaflet-image)                               | leaflet-image.js                                                               |
| - [Condensed attribution](https://github.com/route360/Leaflet.CondensedAttribution)     | leaflet-control-condensed-attribution.js                                       |
| - [Distances, linear referencing](https://github.com/makinacorpus/Leaflet.GeometryUtil) | leaflet.geometryutil.js                                                        |
| [Aynsc library](https://caolan.github.io/async/)                                        | async.js                                                                       |
| [TopoJSON](https://github.com/topojson/topojson)                                        | topojson.min.js                                                                |
| - TopoJSON GridLayer: Created from Leaflet.GeoJSONGridLayer                             | topoJSON.js, TopoJSONGridLayer.js                                              |
| - [PouchDB JavaScript database](https://pouchdb.com/)                                   | pouchdb.js                                                                     |
| [D3 v4 and D3 export](https://d3js.org/)                                                | d3.v4.min.js, canvas-toBlob.js, FileSaver.min.js                               |                                                                                                          
| [Turfjs modular geospatial engine](https://github.com/Turfjs/turf)                      | turf.min.js                                                                    |
| [Proj4js coordinare transformer](https://www.npmjs.com/package/proj4)                   | proj4.js                                                                       |
| [JSON5 parser](https://github.com/json5/json5)                                          | json5.js                                                                       |
| [Simple Statistics](https://simplestatistics.org/)                                      | sstatistics.js                                                                 |
| [Save html2canvas screenshots](https://github.com/niklasvh/html2canvas)                 | html2canvas.js                                                                 |
| [Parse, validate, manipulate, and display dates and times](https://momentjs.com/)       | moment.min.js                                                                  |
	
* Angular 1.5.8 and Leaflet 1.0.3 are many releases behind current; this is deliberate to avoid stability issues 
  during development. At the re-start of the RIF 4.0 development a progressive upgrade to current should be 
  considered;
* TopoJSON GridLayer created from [Leaflet.GeoJSONGridLayer](https://github.com/ebrelsford/leaflet-geojson-gridlayer) by Eric Brelsford 
* There is considerable overlap between the libraries. Turf could probably do most of geospatial functions

# The main module

rifm-app: The definition of the RIF angular module. This is where the "$stateProvider" states URLs are defined. 
These broadly relate to the dashboards selected by the tabs in the main RIF toolbar. All subsequent 
angular code chains to: angular.module("RIF").

The principal purpose of the main module is to control state using the 
[UI Route state machine](https://github.com/angular-ui/ui-router/wiki) and:
  
* Save/restore SelectStateService as required;
* Adds window.console to Internet Explorer to prevent browser hangs caused by no debugger/browser console 
  in un-IE safe code

## CSS

The RIF specific CSS is a bit of a mess and may have a lot of redundancy. This was inherited from the old 
RIF prototype. Neither DM or PH had time to go through it properly. How the split containers refresh on 
resize and on browser resize is now much improved. The map tables do not have ui-layout and do not resize 
correctly. This is a known issue: [#154](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/154) 
in ui-layout and not the RIF. IT is probably best fixed by fitting UI-layout to the map tables.

##  Alerts

Alerts are handled in **rifc-util-alert**, which is right up at the root (on the body of index.html) of all 
the so easily available by inheritance to all other controllers/scopes in the RIF. The use of inheritance 
does not work in services modules and can be hard to predict especially for directives where they are re-used. 
A service was therefore created: **rifs-util-alert**. **rifc-util-alert** uses the html attribute: 
notifications-bar. Some of the errors thrown by the middleware can be a bit weird and not informative. The 
middleware should provide more human readable error messages.

Alerts are of two types: permanent (which have to be closed) and auto closing after five seconds. There is a modal 
from the submission modal which can view all alerts for a session. 

Alerts and debug messages are logged to the middleware in: ```FrontEndLogger.YYYY-MM-DD-n.log```

# Dashboards

Each dashboard for a user session has its state stored in a service (e.g. **rifs-dmap-mappingstate**). This is 
a singleton (with closure) and is not destroyed during a RIF session. The state service are used to either 
restore a user's choices on state changes (which destroys scope and controllers), but also to restore the 
RIF defaults if needed. The save/load study functions reads and writes to these singleton
states.

Each dashboard also has an html partial (e.g. **rifp-dmap-main**) which renders in div defined in *index.html* 
with the data-ui-view attribute. Changing the tab on the main RIF tab bar changes the state 
(**rifc-util-tabctrl**) as defined in the module (**rifm-app**).

Each dashboard has a main controller. These are often quite large and could be refactored in the future, in 
particular there is a lot of functionality that could be moved into services. For example, it is not very 
'angular' to have code that does not deal directly with the UI in the controller. In general, code has been
moved into new services, e.g. **rifs-util-rif40-num-denom.js**. Specific directives usually refer to a unique feature such as a D3 plot 
(e.g. **rifd-dmap-d3rrzoom**).

As an example, the *Rif40NumDenomService* service in **rifs-util-rif40-num-denom.js** contains the code to initialise the
data required for the study submission. The *initialise()* returns a promise so the controller can wait for 
initialisation. This leads to the following recommendations:

* Minimise the number of REST calls to simplify modal initialisation in the controller by creating more
  complex JSON data structures in the middleware, using the [org.json](https://stleary.github.io/JSON-java/) 
  Java library. This avoids the "waterfall of promises" needed to the ensure REST code executes in the correct 
  order;
* Ideally each REST call shouild have its own service wrapper;
* Keep all the promises code together and call functions to make the control flow easy to understand;
* Factor out non-controller UI-related code into new/existing services. 

Where there is shared functionality between dashboards, this is usually found in the utils directory. For 
example, choropleth mapping (**rifc-util-choro**), basemaps (**rifc-util-basemap**, available maps are 
defined in **rifs-util-basemap**), notifications (**rifc-util-alert**) etc.

## Login

**rifc-login-login**: Calls the login method for the RIF database. Ensures all states (i.e. previous user 
inputs) are reset, initialises the taxonomy service. On success we transition to "state1", i.e. the submission 
dashboard. On failure, we remain with the login page.

The logout method (click on the running man on the tool bar) is handled by (**rifc-util-tabctrl**) via a 
yes/no modal.

Note that:

* Refresh logon you off: 
  [issue #113](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/113)
* As will the back button

Although there is little to be done about refresh, the back button should be trappable and the users asked 
if it is OK.

## Submission

**rifc-dsub-main**: Initially makes several chained calls to the database to fill all drop-downs etc depending 
on the user. Once loaded, several other controllers are responsible for entering user defined submission data. 
These all appear in bootstrap modal pop-ups. Each "tree" has at least its own controller, often multiple 
controllers.

The results of user selections are stored in the relevant states (see the submission/services). On clicking 
"Run", we use these states to populate a lump of JSON (**rifs-dsub-model**). This JSON object is then posted 
to the database with the submitStudy method in **rifs-back-requests** as submissionFile.txt. No actual 
calculations are done by the front-end. The save (**rifc-dsub-save**), reset (**rifc-dsub-reset**) and 
open (**rifc-dsub-fromfile**) RIF job methods work with this JSON and the associated state services.

Comparison and Study areas are defined with the same dialog defined by the maptable directive 
(**rifd-dsub-maptable**) launched by either the respective **rifc-dsub-comparea** or **rifc-dsub-studyarea** 
controllers. See **rifs-dsub-model** for how the areas selected are submitted - there is a key for "map_areas" 
in the JSON object relating to either study area or comparison area, within this an area object is stored with 
attributes: id, gid, label and band (for risk analysis).

So far this band attribute is not used as risk analysis cannot be done by the RIF backend yet. 
**rifs-dsub-model** also contains slots that vary depending if the study type is disease mapping or risk 
analysis (disease_mapping_study, risk_analysis_study). As risk mapping has not been done yet, how the two 
different study types are recognised may need to be thought through again. The database cannot handle 
"risk_analysis_study" and will throw an error. All this depends on how risk analysis will be handled in the 
database and middle ware.

Using *rifd-dsub-maptable*, areas can be selected at the required resolution:

* By clicking on the table or map (synced with a $watchCollection)
* By drawing polygons and concentric circles (uses the LeafletDraw library modified in **rifs-util-leafletdraw** to broadcast on
  *$rootScope*)
* By uploading a csv list of districts in the format: [ID, band] (**rifc-dsub-idlist**)
* By selecting with a zipped shapefile (see **rifd-dsub-risk**). Various methods are possible; defining buffers 
  around a point file, defining selection by polygon extent, defining selection by polygon attribute.
* By selecting by postal code (see *rifd-dsub-postal.js*). Postal code, grid X/Y and WGS co-ordinates are
  supported;
* Note that selection based on shapefile/polygon overlay is determined on the basis of intersection with the 
  districts centroid and the overlay. Essentially a simple point-in-polygon test: **rifs-util-gis**.
* Study area for risk analysis allows 1:6 bands, disease mapping just one band. Comparison areas always only 
  allow one band. It is possible to select comparison areas using shapefiles;
* Methods to select layers will be refined once we have some user feedback

The *CommonMappingStateService* (see: *rifs-util-mapstate.js*) stores map state, and removed a lot of code
from the map table directive and maintains selected areas lists in both list and hash forms for efficient 
access. It is also used to implement intersection analysis. See also *rifs-dmap-mappingstate.js* which will 
be merged in time.

Investigation parameters is where the taxonomy service is used. This is a standalone service due to copyright 
issues with the ICD10 codes (we cannot distribute this on github). ICD 9 and 10 are the only services available; 
more taxonomies are planned to be incorporated. This dialog can now handle multiple covariates and additonal 
covariates. In future the modal could use set lists of ICD codes and support multiple investigations. The 
taxonomy list is a ui-grid table. UI-grid now supports "hierarchial" rows (in beta) and so the display could 
be improved to show a hierarchy, e.g. for *C34*:

* C: All malignant neoplasms
  * C30-C39: Malignant neoplasms of respiratory and intrathoracic organs  
    * C30: Malignant neoplasm of nasal cavity and middle ear  
    * C31: Malignant neoplasm of accessory sinuses  
    * C32: Malignant neoplasm of larynx  
    * C33: Malignant neoplasm of trachea  
    * C34: Malignant neoplasm of bronchus and lung  
      * C34.0: Main bronchus
	  * C34.1: Upper lobe, bronchus or lung
	  * C34.2: Middle lobe, bronchus or lung
	  * C34.3: Lower lobe, bronchus or lung
	  * C34.8: Overlapping lesion of bronchus and lung
	  * C34.9: Bronchus or lung, unspecified
    * C37: Malignant neoplasm of thymus  
    * C38: Malignant neoplasm of heart, mediastinum and pleura  
    * C39: Malignant neoplasm of other and ill-defined sites in the respiratory system and intrathoracic organs  

Again, several calls are made to the middle ware (in **rifc-dsub-params**) to fill the drop-downs and results 
are saved in **rifs-dsub-paramstate** for submission. This part of the front-end will probably need the most 
work after the backend and middleware are updated.

Statistical methods (**rifc-dsub-stats**) selects which smoothing method to run. Possible methods are defined in
the middleware (not hard-typed). So far we have HET, BYM, CAR and none. It is also possible to store parameters 
for the model, but this is not used at the moment.

Prior to submission, we can get an HTML formatted summary of the RIF job (**rifc-dsub-summary**) using the tags 
in **rifc-dsub-model** area table function. The model here is of JSON structure and is posted as a text file. 
To get the structure of this, use the 'save' option on the GUI to download a current job as a .json file. The 
model json structure has changed slightly in detail during RIF development, all changes are generally back 
compatible.

On submission, the job is run in the database behind the scenes. Clicking status (**rifc-dsub-status**) gives 
the status for a user's RIF jobs. Status is also checked every 4 seconds to give a notification of completion 
of any pending jobs (this is done in by the tab controller **rifc-util-tabctrl**). This can report a study 
completing twice, this is because the main timer loop is "stacking" up and the code needs to be modified to 
prevent this.

## Mapping and Viewer

These dashboards are very similar so will be explained together. During development, I could not get a straight 
answer as to what the difference with these should be and what actually is required, the two-tab approach is 
directly taken from Fred Fabbri's initial prototype. Hopefully with some user feedback, the functionality of 
these should be refined.

The main difference is that the viewer allows a map and table view, while the mapper allows two maps and focuses
on "bow-tie" plots of risks. Both allow choropleth mapping (**rifc-util-choro**) and changeable basemaps in 
Leaflet (**rifc-util-basemap**). Mapping and Viewer dashboards have their own main controllers 
(**rifc-dmap-main** and **rifc-view-viewer**), but the mapping itself is governed by the **rifc-util-mapping** 
controller as functionality is shared. The appropriate Leaflet container (div) is referenced as either
"diseasemap1", "diseasemap2" or "viewermap".

Polygons are served up in topoJSON tiles as created by Peter's tilemaker as an *L.topoJsonGridLayer*. This is 
very similar to handling normal geoJSON, just the call to the middle ware must be defined correctly: getTileMakerTiles. Once loaded, the tiled layer behaves as
any geoJSON in Leaflet. This has since been extended to use bitmap tiles for all the areas in the geolevel not 
selected/part of a study. This code has only been fitted to the study and comparison selectors and requires 
more work to handle mous events efficiently. 

The mapping initialisation code in the study and comparison area selector modals has been re-written as chained
of promises and associated promise-ified functions. This has removed asynchronous races and this code is now more 
reliable. This also needs to be imnplemented for the mapping and viewer screens and the remaining mapping related
code refactored into services. The asynchronous races in the map and viewer screens are the reason why the maps
sometimes do not zoom to the selection and why automatic choropleth setup does not work.

There are a selection of mapping and selection tools as directives, see **rifd-util-leafletTools**. There are 
also tools to save the D3 plots to png (**rifd-util-savechart**). There exists a directive to save the Leaflet 
map with overlays in **rifd-util-leafletTools** (leafletToPng). This does not work very well and is 
inconsistent between browsers, it will need looking at. I think that it should actually be removed in favour 
of the export tools (explained later) or prompting the user to go full screen and use the screen dump (Print 
Scrn button) to clipboard.

(Note that the directive used for the map area submissions, **rifd-dsub-maptable** duplicates a lot of the 
functionality used here. It was always the intention to refactor **rifd-dsub-maptable** to be more consistent 
with **rifc-util-mapping** and its associated directives).

All D3 plots are dealt with using directives to define a new HTML element. Note that these still need wrapping 
in a *$watch* to allow them to refresh. Capturing the keyboard events in angular for multiple maps and D3 
graphs was quite challenging, there may still be bugs here.

The processed study being mapped are selected via the drop-downs. There is an info button next to these to 
display study details (**rifd-util-info**). This needs some work still on the back-end as not all of the 
relevant information is stored and/or retrieved, see the method getDetailsForProcessedStudy.

Choropleth mapping uses colour scales defined by colorbrewer.org (**rifs-util-colorbrewer**). Category 
definitions follow the usual methods; equal interval, quantile etc (**rifs-util-choro**). The code to do the 
rendering is a bit messy and could do with a tidy (**rifc-util-choro**)

## Export

To get all the results out of the RIF in a zip file (**rifc-expt-export**). All the zipping etc. is done in the 
middle ware. So far this exports the map table (RIF results) and the extract table (data used by the RIF to 
make the results) as csv files. The study and comparison areas are also exported as geoJSON files (text, can 
be loaded into any GIS and converted to a shapefile). Other tables could be exported here too, e.g. a 
formatted table for input to SatScan.

This dashboard also allows a quick preview of the data and the map areas. This would need modification after 
user feedback.

After user testing, there will be loads of requests as to what needs to be outputted ("what-the-old-RIF-did" 
etc. etc.)

An important change will be to save the MappingStateService (**rifs-dmap-mappingstate.js**) saved zoom latitude and
longitude and also the choropleth map setting (ChoroService **rifs-util-choro.js**) in the database so they can be 
used to render the midleware maps as they are setup by the user.

## Backend

Deals with everything related to the connection with the RIF postgres or sqlserver databases via the Java 
middleware.

* **rifs-back-database**: A service storing which database is being used (postgres or ms sqlserver). This is 
  defined in the rifServces.war (resources > properties file) and is therefore not editable from the front-end. 
  This may not be needed in future depending on how the middle ware is refactored, i.e the /ms or /pg may be 
  dropped from the URLs.

* **rifs-back-interceptor**: A "$httpInterceptor" service to deal with outgoing RIF specific requests. This 
  checks if a user is logged in and checks for a 200 response on return of promise. Handles any problems with 
  the calls.

* **rifs-back-requests**: Contains ALL the requests to the middle ware for information from the database for 
  the whole RIF. These return a promise (in most cases).

* **rifs-back-urls**: Contains the constants for the base URLs to the rifServices. These can be edited here depending on the localhost used.
  See also RIF wiki set up instructions.

# Utilities

## Controllers

| File name                       | Description                                                                       |                                                                                                                                                    
|---------------------------------|-----------------------------------------------------------------------------------|
| rifc-util-alert.js              | Alert bars and notifications over whole application                               |
| rifc-util-basemap.js            | Basemap selection modal                                                           |
| rifc-util-choro.js              | Choropleth map symbology modal used by viewer and mapper                          |
| rifc-util-mapping.js            | Map panels                                                                        |
| rifc-util-multiselect.js        | Re-enables multiple selections on ui-grids. Selections on ui-grid are disabled    |
| rifc-util-tabctrl.js            | Handles tab transitions, logout and alert on new study completion                 |

## Directives

| File name                       | Description                                                                       |                                                                                                                                                    
|---------------------------------|-----------------------------------------------------------------------------------|
| rifd-util-d3riskGraph.js        | Risk Graph                                                                        |
| rifd-util-d3riskGraph3.js       | Common Risk Graph                                                                 |
| rifd-util-info.js               | Get info on a completed study - also covariates loss and homogeneity report       |
| rifd-util-leafletTools.js       | Shared Leaflet tools                                                              |
| rifd-util-preventRight.js       | Prevents right click in Leaflet maps                                              |
| rifd-util-savechart.js          | Save D3 charts to a PNG                                                           |

## Partials

| File name                       | Description                                                                       |                                                                                                                                                    
|---------------------------------|-----------------------------------------------------------------------------------|
| rifp-util-basemap.html          | Basemap modal                                                                     |
| rifp-util-choro.html            | Symbology modal                                                                   |
| rifp-util-info.html             | Study info modal                                                                  |
| rifp-util-yesno.html            | Yes-No user choice modal                                                          |

## Services

| File name                       | Description                                                                       |                                                                                                                                                    
|---------------------------------|-----------------------------------------------------------------------------------|
| rifs-util-alert.js              | Alerts functions. Calls Alert controller                                          |
| rifs-util-basemap.js            | Supply pre-defined basemaps tiles to leaflet                                      |
| rifs-util-choro.js              | Render choropleth maps using Colorbrewer                                          |
| rifs-util-colorbrewer.js        | Colorbrewer (http://colorbrewer.org/)                                             |
| rifs-util-d3charts.js           | D3 charts helper functions                                                        |
| rifs-util-exceptionOverwrite.js | Override Angular exception handling, use the AlertService: disabled breaks Firefox|                    
| rifs-util-gis.js                | Basic GIS operations                                                              |
| rifs-util-JSON.js               | JSON - text conversions                                                           |
| rifs-util-leafletdraw.js        | Extend leafletdraw library                                                        |
| rifs-util-mapping.js            | Mapping helper functions                                                          |
| rifs-util-mapstate.js           | Stored map state: see also rifs-dmap-mappingstate.js which will be merged in time |
| rifs-util-maptools.js           | Adds tool icons to leaflet containers                                             |
| rifs-util-projection.js         | CRSS database - get projection from SRID                                          |
| rifs-util-rif40-num-denom.js    | Numerator denominator pairs by geography and health theme                         |
| rifs-util-selectstate.js        | Store state of selection modal                                                    |
| rifs-util-uigrid.js             | UI-Grid helper functions                                                          |

# Front End Issues

There are [53 open issues](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues) at 17/4/2019. Of these, 
25 are relevant to the front end

## Faults

* [Issue #154](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/154) Cannot resize map table 
  (study/comparison area) grids;
  File: rifd-dsub-maptable.js; function onModalResize(). UI-grid resize does not work when you change 
  $scope.gridOptions.minRowsToShow with a window resize. The sizing is OK if you go in and out of the modal, UI grid is not 
  setting the new height.This is UI-grid issue 2531: [angular-ui/ui-grid#2531](https://github.com/angular-ui/ui-grid/issues/2531). UI-layout used in the viewer/mapper appears to work

* [Issue #151](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/151) Internet Explorer (IE) caching;
  Resolution of issue #75 highlighted an issue Internet Explorer (IE) caching. This cannot be detected from the REST result data. The only sensible way to resolve this would be to get the isLoggedIn REST call to return a date time string as well. This would probably prevent IE caching and could be checked to detect the caching and warm the user.

  This will impact any REST call that returns the same data multiple times and so appears to not change but actually does (e.g. 
  login status, rif40_num_denom data)
  
* [Issue #113](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/113) Refresh logs you off.

  When the user refreshes angular goes to state 0 which effectively logs you off. State 0 needs to ask the database if the user 
  (saved as a cookie) is still logged on the session will resume in state1 (study submission screen);
  
* [Issue #57](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/57) Front end mapping synchronisation 
  fails sometimes when you change the study. The auto choropleth setup has been sisabled; the long term fix is discussed above;
  
## Major Enhancements

* [Issue #125](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/125) Add support for region-specific 
  details around point sources. This is [Pooled or individual analysis for multiple risk analysis points/shapes (e.g COMARE postcodes)](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/129) development.
* [Issue #121](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/121) Add Prior sensistivity analysis.
  This will allow users to change the priors used in Bayesian smoothing;
* [Issue #85](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/85) Information Governance tool;
* [Issue #84](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/84) Data loader tool;
* [Issue #83](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/83) Support of statistical packages;

## Minor Enhancements

* [Issue #134](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/134) D3 graphs; todo:

  1. Convert to info and viewer risk graphs to using common version with a single data source. Rename *rifd-util-d3riskGraph3.js* 
     to *rifd-util-d3riskGraph.js* and remove *rifd-util-d3riskGraph2.js*;
  2. Remove non d3 code from *rifs-util-d3charts.js* (functionality now in *rifd-util-d3riskGraph3.js*), rename to 
     *rifs-util-d3riskgraph.js*;
  3. Convert rr-zoom, dist-histo and pyramid directive so the d3 code is in a utils service and now use same the methods as 
     *rifd-util-d3riskGraph.js*. Resizing should now work!
  4. Remove rrZoomReset anti memory leak functionality. It should no longer be needed;
  5. Multiple redraws in the mapping panes should be remove when the fetch code is all converted to use promises;
  6. Add rr-zoom, dist-histo and pyramid to the info modal;
  7. Add "NONE" to second gender selector in info risk graph. The work around is to set both to the same;

* [Issue #133](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/133) DM. Data viewer and Mapping 
   tabs. Cannot select individual study areas within the overall study area;
   
* [Issue #130](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/130) Risk analysis fault/issues:
  
  3. Using add by postcode produces errors on its own, but works;
  4. Errors if nonsensical exposure bands are selected;
  5. Clear does not work after restore from file;
  6. Adding a point produces errors after restore from file;
  7. Add disableMouseClicksAt from frontEndParameters.json5 to replace hard coded 5000 in Tile generation;
  8. Load list from text file loads OK but does not display correctly;
  9. Need a file type filter when loading JSON files;
  10. Zip shapefile load to be able to cope with projections other than 4326 (e.g. local grid).
  
* [Issue #123](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/123) Improve search in taxonomies on 
  Investigation Parameters screen:
  
  The taxonomy search can be confusing when users search by an ICD code. The search currently finds any occurrence of the 
  entered string anywhere in either the code or the description. It is proposed to split the search into two:

  * A search of the code field (or "Term Name", as it appears on screen.
  * A full-text search of the description field.
  
  The UI-Grid module used to implement this now supports "tree" tables to provide a hierarchical display
  
* [Issue #120](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/120) Add model diagnostic tools 
   (in the viewer and exportable as output);
  
* [Issue #89](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/89) Local caching of base maps.

  This is so that we display the underlying map details when the RIF is running on a secure network without access to the 
  internet. Will need a webapp for the files, a downloader tool and the front end URLs changed to be a local version.

* [Issue #78](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/78) Risk Analysis selection at high 
  resolution (e.g. MSOA) does not perform acceptably. Tiles have been generated using GeoTools and the study/comparision area 
  selection modals converted to use the PNG tiles for areas not selected. Once GeoJSON mouse over support (issue #66) has been 
  fixed this code can then be migrated to the viewer and mapping modals.
  
  Currently when selecting only the GeoJSON grids are used:
  * Once you go review a previous loaded/selected study are by going back into the study area modal you cannot add/remove areas at
    present. This would be complex but possible to fix if you have all the centroids and bounding boxes and can then fetch the area 
    geojson use a new middleware call;
  * Memory requirements will be high, census output area selection will crash all 32bit browsers and Chome (which is not fully 
    64 bit);
  
* [Issue #67](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/67) Print state support. Add support 
  for modifying *print_state* in the front end so the user sees the maps as last viewed. The GeoTools map generator will
  also be able to reproduce the map exactly; including the extents visable;
  
* [Issue #66](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/66) GeoJSON mouse over support with 
  shapefile shapes. GeoJSON hover support with shapefile shapes. This will allow the hover mode to display data from the area 
  geoJSON below the shapefile shapes. I experimented with mouse click through but was unable to get to work without adverse 
  performance implications:
  
  * TopoJSON grid layer blocks mouse clicks unless it is the in view pane;
  * Using the mouse position to find the nearest TopoJSON grid layer does work, but you only get one mouse event per layer 
    boundary cross.  We would need to modify the map shape pane to transmit more events (this may not be possible), this has 
    performance implications as you will need to work out which GeoJSON shape is nearest the click. With tens of thousands of 
    shapes this will be slow, requiring more complexity - e..g use 1km grids to reduce the search;
    
* [Issue #63](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/63) Re-factor of Leaflet mapping 
  code. This is discussed above;
  
* [Issue #61](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/61) Add support for risk analysis in 
  map export;
  
## Fixed, but open as not tested sufficently
 
* [Issue #76](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/76) Study reset sometimes does not 
  reset the stats selection;
  
* [Issue #62](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/62) studyType mismatch for map: 
  viewermap; study ID: ...;
  
* [Issue #56](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/56) Error loading study from database via 
   middleware generated file. This has partially fixed but it needs more work and a lot of testing;

* [Issue #20](https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/issues/20) Parameter validity should be 
   checked in the front end, or at least reported back to it. This particular error has been fixed by the multiple covarate 
   development; but there will be more;


**Peter Hambly, April 2019**