// ************************************************************************
//
// GIT Header
//
// $Format:Git ID: (%h) %ci$
// $Id: 7ccec3471201c4da4d181af6faef06a362b29526 $
// Version hash: $Format:%H$
//
// Description:
//
// Rapid Enquiry Facility (RIF) - geo2TopoJSON - GeoJSON to TopoJSON convertor; method specfic functions
//
// Copyright:
//
// The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
// that rapidly addresses epidemiological and public health questions using 
// routinely collected health and population data and generates standardised 
// rates and relative risks for any given health outcome, for specified age 
// and year ranges, for any given geographical area.
//
// Copyright 2014 Imperial College London, developed by the Small Area
// Health Statistics Unit. The work of the Small Area Health Statistics Unit 
// is funded by the Public Health England as part of the MRC-PHE Centre for 
// Environment and Health. Funding for this project has also been received 
// from the Centers for Disease Control and Prevention.  
//
// This file is part of the Rapid Inquiry Facility (RIF) project.
// RIF is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// RIF is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with RIF. If not, see <http://www.gnu.org/licenses/>; or write 
// to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
// Boston, MA 02110-1301 USA
//
// Author:
//
// Peter Hambly, SAHSU

/*
 * Function:	geo2TopoJSONFieldProcessor()
 * Parameters:	fieldname, val, topojson_options, ofields [field parameters array], 
 *				response object, express HTTP request object, RIF logging object
 * Returns:		Text of field processing log
 * Description: toTopoJSON method field processor. Called from req.busboy.on('field') callback function
 *
 *				verbose: 	Set Topojson.Topology() option if true. 
 *				zoomLevel: 	Set quantization field and Topojson.Topology() option using local function getQuantization()
 * 							i.e. Set the maximum number of differentiable values along each dimension) by zoomLevel
 *
 * 							Zoomlevel		Quantization
 * 							---------		------------
 *
 * 							<=6				400
 * 							7				700
 * 							8				1500
 * 							9				3000
 * 							10				5000
 * 							>10				10000
 *				projection: Set projection field and Topojson.Topology() option. E.g. to convert spherical input geometry 
 *							to Cartesian coordinates via a D3 geographic projection. For example, a projection of 'd3.geo.albersUsa()' 
 *							will project geometry using a composite Albers equal-area conic projection suitable for the contiguous 
 *							United States, Alaska and Hawaii. DO NOT SET UNLESS YOU KNOW WHAT YOU ARE DOING!
 *				id:			Name of feature property to promote to geometry id; default is ID. Value must exist in data.
 *							Creates myId() function and registers it with Topojson.Topology() via the id option
 *				property-transform-fields:
 *							JSON array of additional fields in GeoJSON to add to output topoJSON. Uses the Topojson.Topology()
 * 							property-transform option. Value must be parseable by JSON.parse(). Value must exist in data.
 *							Creates myPropertyTransform() function and registers it with Topojson.Topology() via the 
 *							property-transform option
 *
 * All other fields have no special processing. Fields are returned in the response.fields JSON array. Any field processing errors 
 * either during processing or in the id and property-transform Topojson.Topology() callback functions will cause processing to fail.
 *
 * See NPM tpopjson command line reference: https://github.com/mbostock/topojson/wiki/Command-Line-Reference
 *
 * JSON injection protection. This function does NOT use eval() as it is source of potential injection
 * e.g.					var rval=eval("d.properties." + ofields[fieldname]);
 * Instead it tests for the field name directly:
 *						if (!d.properties[ofields[fieldname]]) { ...
 * So setting formData (see test\request.js test 18) to:
 * formData["property-transform-fields"]='["eval(console.error(JSON.stringify(req, null, 4)))"]';
 * Will cause an error:
 * Field: property-transform-fields[["eval(console.error(JSON.stringify(req, null, 4)))"]];
 * myPropertyTransform() function id fields set to: ["eval(console.error(JSON.stringify(req, null, 4)))"]; 1 field(s)
 * FIELD PROCESSING ERROR! Invalid property-transform field: d.properties.eval(console.error(JSON.stringify(req, null, 4))) does not exist in geoJSON;
 */
geo2TopoJSONFieldProcessor=function geo2TopoJSONFieldProcessor(fieldname, val, topojson_options, ofields, response, req, serverLog) {
	
	scopeChecker(__file, __line, {
		fieldname: fieldname,
		val: val,
		ofields: ofields,
		topojson_options: topojson_options,
		req: req,
		response: response,
		serverLog: serverLog
	});	
	
	var msg,
	    text = "",

/*
 * Function: 	getQuantization() 
 * Parameters:  Level
 * Description: Set quantization (the maximum number of differentiable values along each dimension) by zoomLevel
 *
 * Zoomlevel		Quantization
 * ---------		------------
 *
 * <=6				400
 * 7				700
 * 8				1500
 * 9				3000
 * 10				5000
 * >10				10000
 */
        getQuantization = function(lvl) {
         if (lvl <= 6) {
            return 400;
         } else if (lvl == 7) {
            return 700;
         } else if (lvl == 8) {
            return 1500;
         } else if (lvl == 9) {
            return 3000;
         } else if (lvl == 10) {
            return 5000;
         } else {
            return 10000; // Default
         }
        };
	
	if ((fieldname == 'verbose')&&(val == 'true')) {
		topojson_options.verbose = true;
	}
	else if (fieldname == 'zoomLevel') {
	   topojson_options.quantization = getQuantization(val);
	   text+="Quantization set to: " + topojson_options.quantization;
	   ofields["quantization"]=topojson_options.quantization;
	}
	else if (fieldname == 'projection') {
	   topojson_options.projection = val;
	   text+="Projection set to: " + topojson_options.projection;
	   ofields["projection"]=topojson_options.projection;
	}
	else if (fieldname == 'id') {				
		text+="\nmyId() function id field set to: " + val;
		ofields[fieldname]=val;				
//
// Promote tile gid to id
//					
		ofields.myId = function(d) {
// Dont use eval() = it is source of potential injection
// e.g.					var rval=eval("d.properties." + ofields[fieldname]);
			if (!d.properties[ofields[fieldname]]) { // Dont raise errors, count them up and stop later
				response.field_errors++;
				var msg="FIELD PROCESSING ERROR! Invalid id field: d.properties." + ofields[fieldname] + " does not exist in geoJSON";
				if (topojson_options.id) {
					serverLog.serverLog2(__file, __line, "req.busboy.on('field')", msg, req);	
					topojson_options.id = undefined; // Prevent this section running again!	
					response.message = response.message + "\n" + msg;
				}
			}
			else {
				return d.properties[ofields[fieldname]];
			}
//					response.message = response.message + "\nCall myId() for id field: " + ofields[fieldname] + 
//						"; value: " + d.properties[ofields[fieldname]];									
		}						
		topojson_options.id = ofields.myId;				
	}
	else if (fieldname == 'property-transform-fields') {	
		var propertyTransformFields;
		ofields[fieldname]=val;	
		try {
			propertyTransformFields=JSON.parse(val);
			text+="\nmyPropertyTransform() function id fields set to: " + val + 
				"; " + propertyTransformFields.length + " field(s)";
//
// Property transform support
//					
			ofields.myPropertyTransform = function(d) {
// Dont use eval() = it is source of potential injection
//e.g.				var rval=eval("d.properties." + ofields[fieldname]);
				var rval={}; // Empty return object
				for (i = 0; i < propertyTransformFields.length; i++) {
					if (!d.properties[propertyTransformFields[i]]) { // Dont raise errors, count them up and stop later
						response.field_errors++;
						var msg="FIELD PROCESSING ERROR! Invalid property-transform field: d.properties." + propertyTransformFields[i] + 
							" does not exist in geoJSON";
						if (topojson_options["property-transform"]) {
							serverLog.serverLog2(__file, __line, "req.busboy.on('field')", msg, req);	
							topojson_options["property-transform"] = undefined; // Prevent this section running again!	
							response.message = response.message + "\n" + msg;
						}
					}
					else {
						rval[propertyTransformFields[i]] = d.properties[propertyTransformFields[i]];
					}
//								response.message = response.message + "\nCall myPropertyTransform() for property-transform field: " + 
//									propertyTransformFields[i] + 
//									"; value: " + d.properties[propertyTransformFields[i]];									
				}						
				return rval;
			};
			topojson_options["property-transform"] = ofields.myPropertyTransform;	
		}
		catch (e) {
			response.field_errors++;
			msg="FIELD PROCESSING ERROR! field [" + fieldname + "]: " + val + "; invalid array exception";
			response.message = msg + "\n" + response.message;
			serverLog.serverLog2(__file, __line, "req.busboy.on('field')", msg, req);							
		}
	}
	else {
		ofields[fieldname]=val;	
	}	
	
	return text;
}

/*
 * Function:	bigToSstring()
 * Parameters:	Buffer, internal reposnse object, d object
 * Returns:		Buffer converted to string
 * Description: Convert buffer to string in sections avoiding below error...
 *				It does not avoid the error, but it does demonstate the 256M limit JSON to string conversion limit nicely
 */
bigToSstring=function bigToSstring(data, response, d) {
	
	scopeChecker(__file, __line, {
		data: data,
		response: response,
		d: d
	});	
	
	try {		
		var str="";
		var nstr;
//		var strArray = [];
		var pos=0;
		var i=0;
		var len=1024*1024; // 1MB chunks
		var kStringMaxLength = process.binding('buffer').kStringMaxLength;
		
		do {
			i++;
//			strArray.push(data.slice(pos, pos+len).toString());
			str+=data.slice(pos, pos+len).toString();
			nstr=str;
			str=nstr;
			nstr=undefined;
//			console.error("strArray: " + strArray.length + "; data: " + data.length + "; pos: " + pos + "; i: " + i);
//			console.error("str: " + str.length + "; data: " + data.length + "; pos: " + pos + "; i: " + i);
			pos+=len;		
		}
		while (pos < data.length);

//		console.error("End strArray: " + strArray.length + "; data: " + data.length + "; pos: " + pos + "; i: " + i);
//		str=strArray.join("");
//		console.error("End str: " + str.length + "; data: " + data.length + "; pos: " + pos + "; i: " + i);
	} catch (e) {                            // Catch conversion errors
		var msg;
		
		if (e.message == "Invalid string length" && data.length > kStringMaxLength) {
			msg="Unable to convert buffer to string\nCaught error: " + e.message + 
				"; maximum string length: " + kStringMaxLength + " is exceeded by data buffer length: " + data.length + 
				"\nStack >>>\n" + e.stack + "\n<< End of stack";
		}
		else {
			msg="Unable to convert buffer to string\nCaught error: " + e.message + "\nStack >>>\n" + e.stack + "\n<< End of stack";	
		}
		msg+="\nYour input file " + d.no_files + ": " + 
			d.file.file_name + "; size: " + d.file.file_data.length + 
//			"\nstr: " + str.length + "; data: " + data.length + "; pos: " + pos + "; i: " + i + "; maximum string length: " + kStringMaxLength +
			";\n" + msg + ": \n" + "Debug message:\n" + response.message + "\n\n" ;
		if (d.file.file_data.length > 0) { // Add first 240 chars of file to message
			var truncated_data=d.file.file_data.toString('ascii', 0, 240);
			if (!/^[\x00-\x7F]*$/.test(truncated_data)) { // Test if not ascii
				truncated_data=d.file.file_data.toString('hex', 0, 240); // Binary: display as hex
			}
			if (truncated_data.length < d.file.file_data.length) {
				msg=msg + "\nTruncated data:\n" + truncated_data + "\n";
			}
			else {
				msg=msg + "\nData:\n" + truncated_data + "\n";
			}
		}
			
		response.file_errors++;					// Increment file error count		
		response.message = msg + "\n" + response.message;	
		response.error = e.message;
				
		return undefined;
	} 	
	
	return str;
} // End of bigToSstring()

/*
 * Function:	jsonParse()
 * Parameters:	Buffer, internal reposnse object
 * Returns:		Buffer parsed to JSON
 * Description: Parses buffer as a feature collection feature by feature to avoid toString() errors where the string is over 255MB
 */
jsonParse = function jsonParse(data, response) {

	scopeChecker(__file, __line, {
		data: data,
		response: response
	});	
	
	var lstart = new Date().getTime();	
	var myFeatureCollection;
	
	if (data.indexOf('{"type":"FeatureCollection","features":[', 0) == 0) { // Its a feature collection
		response.message+="\nParsing feature collection; data length: " + data.length;
			
		var featureOffset = [0];
		var newOffset;
		var offset=0;
		do { // Look for feature collection start: {"type":"Feature"
			newOffset=data.indexOf('{"type":"Feature",', offset);
			if (newOffset >= 0) {
				featureOffset.push(newOffset);
				offset=newOffset+1;
			}
		} while (newOffset >= 0);
		
		var parseAble;
		var parseError;
		var parsedJson=[];
		var totalParseTime=0;
		for (var i=0; i<featureOffset.length; i++) {
			parsedJson[i] = {
				json: undefined,
				featureOffset: featureOffset[i],
				offsetEnd: undefined,
				parseError: undefined,
				parseTime: undefined
			};
			// Look for end of feature collection end: ]]}}
			parsedJson[i].offsetEnd=data.indexOf(']]}}', featureOffset[i])+4;
		}
		
		for (var i=0; i< parsedJson.length; i++) { // Parse using featureOffset and offsetEnd
			var buf=data.slice( parsedJson[i].featureOffset, parsedJson[i].offsetEnd);
			var str=buf.toString('ascii', 0, 60);
			var str2=buf.toString('ascii', buf.length-60);
			try {
				if (i == 0) { // Add end of feature collection
					str=buf.toString()+"]}";
					parsedJson[i].json=JSON.parse(str);
				}
				else {
					parsedJson[i].json=JSON.parse(buf.toString());					
				}
			}
			catch (e) {
				parsedJson[i].parseError=e.message;
			}
			
			var now = new Date().getTime();
			parsedJson[i].parseTime=(now - lstart)/1000; // in S	
			if (parseError) {
				response.message+="\nWarning feature [" + i + "/" + (featureOffset.length-1) + "] start: " + parsedJson[i].featureOffset + "; end: " + parsedJson[i].offsetEnd + 
					"; Feature length: " + buf.length + 
					"; could not be parsed: " + parseError + "\n>>>\n" + buf.toString('ascii', 0, 240) + "\n<<<\n";
			}
			else if (i<4 || i>(featureOffset.length-4)) {
				if (str.length < 60) {
					response.message+="\nFeature [" + i + "/" + (featureOffset.length-1) + "] start: " + parsedJson[i].featureOffset + "; end: " + parsedJson[i].offsetEnd + 
						"; Feature length: " + buf.length + 
						"\n>>>" + str + "<<<";
				}
				else {
					response.message+="\nFeature [" + i + "/" + (featureOffset.length-1) + "] start: " + parsedJson[i].featureOffset + "; end: " + parsedJson[i].offsetEnd + 
						"; Feature length: " + buf.length + 
						"\n>>>" + str + "<<< ... >>>" + str2 + "<<<";
				}
			}
			else if (((i/1000)-Math.floor(i/1000)) == 0 || parsedJson[i].parseTime > (totalParseTime + 1)) { 
				totalParseTime=parsedJson[i].ParseTime;
				response.message+="\nFeature [" + i	+ "/" + (featureOffset.length-1) + "] start: " + parsedJson[i].featureOffset + "; end: " + parsedJson[i].offsetEnd + 
					"; Feature length: " + buf.length + 
					"; parseAble: " + parsedJson[i].parseAble +
					"\n>>>" + str + "<<< ... >>>" + str2 + "<<<";				
			}
		}

		var end = new Date().getTime();
		var ParseTime=(end - lstart)/1000; // in S			
		if (featureOffset.length == parsedJson.length) {
			response.message+="\nFeature collection parse complete; features detected: " + (featureOffset.length-1) + 
				"; data length: " + data.length + "; took: " + ParseTime + " S";
			myFeatureCollection=parsedJson[0].json;
			for (var i=1; i<parsedJson.length; i++) {
				myFeatureCollection.features[i-1]=parsedJson[i].json;
			}			
		}
		else {
			throw new Error("Feature collection parse failed: " + (featureOffset.length - parsedJson.length) + "/" + featureOffset.length + " features failed to parse");
		}
	}
	
	var kStringMaxLength = process.binding('buffer').kStringMaxLength;
	if (myFeatureCollection) {
		return(myFeatureCollection);
	}		
	else if (data.length < kStringMaxLength) {		
		return JSON.parse(data.toString()); // Parse file stream data to JSON
	}	
	else {
		throw new Error("Cannot parse string: not a feature collection and too long: " + data.length + " for JSON.parse(data.toString()); limit: " + kStringMaxLength);
	}
}

/*
 * Function:	geo2TopoJSONFile()
 * Parameters:	d object (temporary processing data, 
				ofields [field parameters array],
				TopoJSON topology processing topojson_options, 
				my response object
 * Returns:		d object topojson/Nothing on failure
 * Description: TopoJSON processing for files (geo2TopoJSON service):
 *				- converts string to JSON
 *				- calls topojson.topology() using topojson_options
 * 				- Add file name, stderr and topoJSON to my response
 *
 * Modifies/creates:
 *				d.file.jsonData,
 *				d.file.topojson,
 *				d.file.topojson_stderr,
 *				response.message,
 *				response.file_list[d.no_files-1] set to file object:
 *						file_name: File name
 *						topojson: TopoJSON created from file geoJSON,
 *						topojson_stderr: Debug from TopoJSON module,
 *						topojson_runtime: Time to convert geoJSON to topoJSON (S),
 *						file_size: Transferred file size in bytes,
 *						transfer_time: Time to transfer file (S),
 *						uncompress_time: Time to uncompress file (S)/undefined if file not compressed,
 *						uncompress_size: Size of uncompressed file in bytes 
 *
 * On error sets:
 *				response.message,
 *				response.no_files,
 *				response.fields,
 *				response.file_errors,
 *				response.error 
 */
geo2TopoJSONFile=function geo2TopoJSONFile(d, ofields, topojson_options, stderr, response) {
	var topojson = require('topojson');

	scopeChecker(__file, __line, {
		d: d,
		ofields: ofields,
		topojson_options: topojson_options,
		stderr: stderr,
		response: response
	});	
	var msg="";
	
//	response.no_files=d.no_files;			// Add number of files process to response
	response.fields=ofields;				// Add return fields	
	try {	
		d.file.jsonData = undefined;
		// Set up file list response now, in case of exception
		
/* Response array file objects:
 *						file_name: File name
 *						topojson: TopoJSON created from file geoJSON,
 *						topojson_stderr: Debug from TopoJSON module,
 *						topojson_runtime: Time to convert geoJSON to topoJSON (S),
 *						file_size: Transferred file size in bytes,
 *						transfer_time: Time to transfer file (S),
 *						uncompress_time: Time to uncompress file (S)/undefined if file not compressed,
 *						uncompress_size: Size of uncompressed file in bytes
 */
		response.file_list[d.no_files-1] = {
			file_name: d.file.file_name,
			topojson: '',
			topojson_stderr: '',
			topojson_runtime: '',
			file_size: '',
			transfer_time: '',
			uncompress_time: undefined,
			uncompress_size: undefined
		};
		
//		var str=d.file.file_data.replace(/(\r\n|\n|\r)/gm, "");		
//		msg="\nSTR(" + str.length + "): " + d.file.file_data.toString('ascii', 0, 240) + "\nSTR replaced: " + str.toString('ascii', 0, 240);
//		response.message = response.message + msg;	
//		d.file.file_data=str;
		
/* coa2011.js fails with:

Your input file 1: coa11.json; size: 1674722608; does not seem to contain valid JSON: 
Caught error: toString failedStack >>>
Error: toString failed
    at Buffer.toString (buffer.js:382:11)
    at Object.geo2TopoJSONFile (C:\Users\Peter\Documents\GitHub\rapidInquiryFacility\rifNodeServices\lib\geo2TopoJSON.js:292:49)
    at Busboy.<anonymous> (C:\Users\Peter\Documents\GitHub\rapidInquiryFacility\rifNodeServices\lib\nodeGeoSpatialServices.js:571:24)
    at emitNone (events.js:72:20)
    at Busboy.emit (events.js:166:7)
    at Busboy.emit (C:\Users\Peter\Documents\GitHub\rapidInquiryFacility\rifNodeServices\node_modules\connect-busboy\node_modules\busboy\lib\main.js:31:35)
    at C:\Users\Peter\Documents\GitHub\rapidInquiryFacility\rifNodeServices\node_modules\connect-busboy\node_modules\busboy\lib\types\multipart.js:52:13
    at doNTCallback0 (node.js:419:9)
    at process._tickCallback (node.js:348:13)
<< End of stack:

Streaming parser needed; or it needs toString()ing in sections

 */
		
		// Call parser that can process multi GB collections
		d.file.jsonData=jsonParse(d.file.file_data, response);
		
		// Re-route topoJSON stderr to stderr.str
		stderr.disable();
		
		var lstart = new Date().getTime();			
		d.file.topojson = topojson.topology({   // Convert geoJSON to topoJSON
			collection: d.file.jsonData
			}, topojson_options);				
		stderr.enable(); 				   // Re-enable stderr
		str=undefined;
		
		d.file.topojson_stderr=stderr.str();  // Get stderr as a string	
		stderr.clean();						// Clean down stderr string
		stderr.restore();                  // Restore normal stderr functionality 

// Add file stderr and topoJSON to my response
// This will need a mutex if > 1 thread is being processed at the same time
		response.file_list[d.no_files-1].topojson=d.file.topojson;
		response.file_list[d.no_files-1].topojson_stderr=d.file.topojson_stderr;

		var end = new Date().getTime();
		response.file_list[d.no_files-1].topojson_runtime=(end - lstart)/1000; // in S			
		response.file_list[d.no_files-1].file_size=d.file.file_size;		
		response.file_list[d.no_files-1].geojson_length=d.file.file_size;	
		response.file_list[d.no_files-1].topojson_length=d.file.file_size;
		response.file_list[d.no_files-1].transfer_time=d.file.transfer_time;
		response.file_list[d.no_files-1].uncompress_time=d.file.uncompress_time;
		response.file_list[d.no_files-1].uncompress_size=d.file.uncompress_size;
		
		response.file_list[d.no_files-1].topojson_length=JSON.stringify(d.file.topojson).length;
		
		msg+= "Runtime: " + "; topoJSON length: " + response.file_list[d.no_files-1].topojson_length + "]"
		if (d.file.topojson_stderr.length > 0) {  // Add topoJSON stderr to message	
// This will need a mutex if > 1 thread is being processed at the same time	
			response.message = response.message + "\n" + msg + " OK:\nTopoJson.topology() stderr >>>\n" + 
				d.file.topojson_stderr + "<<< TopoJson.topology() stderr";
//			serverLog.serverLog(msg + "TopoJson.topology() stderr >>>\n"  + 
//				d.file.topojson_stderr + "<<< TopoJson.topology() stderr", 
//				req);
		}
		else {
// This will need a mutex if > 1 thread is being processed at the same time
			response.message = response.message + "\n" + msg + " OK";
//			serverLog.serverLog("TopoJson.topology() no stderr; " + msg, 
//				req);		
		}			
															   
		return d.file.topojson;								   
	} catch (e) {                            // Catch conversion errors

		d.file.topojson_stderr=stderr.str();  // Get stderr as a string	
		stderr.clean();						// Clean down stderr string
		stderr.restore();                  // Restore normal stderr functionality 	
		if (!d.file.jsonData) {
			msg="does not seem to contain valid JSON: ";
		}
		else {
			msg="does not seem to contain valid TopoJSON: ";
		}
		msg+="\nCaught error: " + e.message + "\nStack >>>\n" + e.stack + "\n<< End of stack";
		msg+="\nYour input file " + d.no_files + ": " + 
			d.file.file_name + "; size: " + d.file.file_data.length + 
			"; " + msg + ": \n" + "Debug message:\n" + response.message + "\n\n" + 
			"stderr >>>\n" + d.file.topojson_stderr + "\n<<< end of stderr";
		if (d.file.file_data.length > 0) { // Add first 240 chars of file to message
			var truncated_data=d.file.file_data.toString('ascii', 0, 240);
			if (!/^[\x00-\x7F]*$/.test(truncated_data)) { // Test if not ascii
				truncated_data=d.file.file_data.toString('hex', 0, 240); // Binary: display as hex
			}
			if (truncated_data.length < d.file.file_data.length) {
				msg=msg + "\nTruncated data(" + truncated_data.length + "/" + d.file.file_data.length + "):\n" + truncated_data + "\n";
			}
			else {
				msg=msg + "\nData (" + d.file.file_data.length + "):\n" + truncated_data + "\n";
			}
		}
		
		response.file_errors++;					// Increment file error count		
		response.message = msg + "\n" + response.message;	
		response.error = e.message;
				
		return;
	} 	
} // End of geo2TopoJSONFile

module.exports.geo2TopoJSONFieldProcessor = geo2TopoJSONFieldProcessor;
module.exports.geo2TopoJSONFile = geo2TopoJSONFile;	