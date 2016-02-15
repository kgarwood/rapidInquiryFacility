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
// Rapid Enquiry Facility (RIF) - GeoJSON to topoJSON converter webservice
//								  Uses node.js TopoJSON module
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
//
// Usage: tests/requests.js
//
// Uses:
//
// CONVERTS GEOJSON(MAX 100MB) TO TOPOJSON
// Only POST requests are processed
// Expects a vaild geojson as input
// Topojson have quantization on
// The level of quantization is based on map tile zoom level
// More info on quantization here: https://github.com/mbostock/topojson/wiki/Command-Line-Reference
//
// Prototype author: Federico Fabbri
// Imperial College London
//
 
//  Globals
var util = require('util'),
	topojson = require('topojson'),
	zlib = require('zlib'),
    stderrHook = require('../lib/stderrHook'),
    httpErrorResponse = require('../lib/httpErrorResponse'),
    rifLog = require('../lib/rifLog'),
    os = require('os'),
    fs = require('fs'),

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
     },
/*
 * Function: 	TempData() 
 * Parameters:  NONE
 * Description: Construction for TempData
 */
     TempData = function() {
		
		this.file = '';
		this.file_list = [];
		this.no_files = 0;	
		this.myId = '';
		
        return this; 
     },

/*
 * Function:	_process_json()
 * Parameters:	d object (temporary processing data, 
				ofields [field parameters array],
				TopoJSON topology processing options,
				HTTP request object,
				HTTP response object, 
				my response object
 * Returns:		d object topojson/Nothing on failure
 * Description: TopoJSON processing:
 *				- converts string to JSON
 *				- calls topojson.topology() using options
 * 				- Add file name, stderr and topoJSON to my response
 */
	_process_json=function(d, ofields, options, stderr, req, res, response) {
		var msg="File [" + d.no_files + "]: " + d.file.file_name;
		
		response.message = response.message + '\nProcessing ' + msg;	
		try {	
			d.file.jsonData = undefined;
			// Set up file list reponse now, in case of exception
			
/* Array file objects:
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
			d.file.jsonData = JSON.parse(d.file.file_data.toString()); // Parse file stream data to JSON

			// Re-route topoJSON stderr to stderr.str
			stderr.disable();
			var lstart = new Date().getTime();			
			d.file.topojson = topojson.topology({   // Convert geoJSON to topoJSON
				collection: d.file.jsonData
				}, options);				
			stderr.enable(); 				   // Re-enable stderr
			
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
			response.file_list[d.no_files-1].transfer_time=d.file.transfer_time;
			response.file_list[d.no_files-1].uncompress_time=d.file.uncompress_time;
			response.file_list[d.no_files-1].uncompress_size=d.file.uncompress_size;
			
			msg+= "; runtime: " + "; topoJSON length: " + JSON.stringify(d.file.topojson).length + "]"
			if (d.file.topojson_stderr.length > 0) {  // Add topoJSON stderr to message	
	// This will need a mutex if > 1 thread is being processed at the same time	
				response.message = response.message + "\n" + msg + " OK:\nTopoJson.topology() stderr >>>\n" + 
					d.file.topojson_stderr + "<<< TopoJson.topology() stderr";
				rifLog.rifLog(msg + "TopoJson.topology() stderr >>>\n"  + 
					d.file.topojson_stderr + "<<< TopoJson.topology() stderr", 
					req);
			}
			else {
	// This will need a mutex if > 1 thread is being processed at the same time
				response.message = response.message + "\n" + msg + " OK";
				rifLog.rifLog("TopoJson.topology() no stderr; " + msg, 
					req);		
			}			
																   
			return d.file.topojson;								   
		} catch (e) {                            // Catch conversion errors

			stderr.restore();                  // Restore normal stderr functionality 	
			if (!d.file.jsonData) {
				msg="does not seem to contain valid JSON";
			}
			else {
				msg="does not seem to contain valid TopoJSON";
			}
			msg="Your input file " + d.no_files + ": " + 
				d.file.file_name + "; size: " + d.file.file_data.length + 
				"; " + msg + ": \n" + "Debug message:\n" + response.message + "\n\n";
			if (d.file.file_data.length > 0) { // Add first 132 chars of file to message
				var truncated_data=d.file.file_data.toString().substring(0, 132);
				if (!/^[\x00-\x7F]*$/.test(truncated_data)) { // Test if not ascii
					truncated_data=d.file.file_data.toString('hex').substring(0, 132); // Binary: display as hex
				}
				if (truncated_data.length > 132) {
					msg=msg + "\nTruncated data:\n" + truncated_data + "\n";
				}
				else {
					msg=msg + "\nData:\n" + truncated_data + "\n";
				}
			}
		
			response.no_files=d.no_files;			// Add number of files process to response
			response.fields=ofields;				// Add return fields			
			httpErrorResponse.httpErrorResponse(__file, __line, "_process_json()", rifLog, 
				500, req, res, msg, e, response);				
			return;
		} 	
	}; // End of globals

/*
 * Function: 	exports.convert()
 * Parameters:	Express HTTP request object, response object
 * Description:	Express web server handler function for topoJSON conversion
 */
exports.convert = function(req, res) {

	try {
		
//  req.setEncoding('utf-8'); // This corrupts the data stream with binary data
//	req.setEncoding('binary'); // So does this! Leave it alone - it gets it right!

		res.setHeader("Content-Type", "text/plain");
		
// Add stderr hook to capture debug output from topoJSON	
		var stderr = stderrHook.stderrHook(function(output, obj) { 
			output.str += obj.str;
		});
		
/*
 * Response object - no errors:
 *                    
 * no_files: 		Numeric, number of files    
 * field_errors: 	Number of errors in processing fields
 * file_list: 		Array file objects:
 *						file_name: File name
 *						topojson: TopoJSON created from file geoJSON,
 *						topojson_stderr: Debug from TopoJSON module,
 *						topojson_runtime: Time to convert geoJSON to topoJSON (S),
 *						file_size: Transferred file size in bytes,
 *						transfer_time: Time to transfer file (S),
 *						uncompress_time: Time to uncompress file (S)/undefined if file not compressed,
 *						uncompress_size: Size of uncompressed file in bytes
 * message: 		Processing messages, including debug from topoJSON               
 * fields: 			Array of fields; includes all from request plus any additional fields set as a result of processing 
 */ 
		var response = {                 // Set output response    
			no_files: 0,    
			field_errors: 0,
			file_list: [],
			message: '',               
			fields: [] 
		};
		var d_files = { 
			d_list: []
		}
			
	// Post method	
		if (req.method == 'POST') {
	 
	// Default topojson options 
			var options = {
				verbose: false,
				quantization: 1e4		
			};
		
	// Default return fields	
			var ofields = {
				my_reference: '', 
				zoomLevel: 0, 
				verbose: false,
				quantization: options.quantization,
				projection: options.projection
			};	

/*
 * Function: 	req.busboy.on('filesLimit') callback function
 * Parameters:	None
 * Description:	Processor if the files limit has been reached  
 */				  
			req.busboy.on('filesLimit', function() {
				var msg="FAIL! Files limit reached";
				response.no_files=d.no_files;			// Add number of files process to response
				response.fields=ofields;				// Add return fields	
				httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('filesLimit')", 
					rifLog, 500, req, res, msg, undefined, response);
				return;				
			});
/*
 * Function: 	req.busboy.on('partsLimit') callback function
 * Parameters:	None
 * Description:	Processor if the parts limit has been reached 
 */				  
			req.busboy.on('partsLimit', function() {
				var msg="FAIL! Parts limit reached";
				response.no_files=d.no_files;			// Add number of files process to response
				response.fields=ofields;				// Add return fields	
				httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('partsLimit')", 
					rifLog, 500, req, res, msg, undefined, response);
				return;				
			});
/*
 * Function: 	req.busboy.on('fieldsLimit') callback function
 * Parameters:	None
 * Description:	Processor if the fields limit has been reached  
 */				  
			req.busboy.on('fieldsLimit', function() {
				var msg="FAIL! Fields limit reached";
				response.no_files=d.no_files;			// Add number of files process to response
				response.fields=ofields;				// Add return fields	
				httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('fieldsLimit')", 
					rifLog, 500, req, res, msg, undefined, response);
				return;				
			});
			
/*
 * Function: 	req.busboy.on('file') callback function
 * Parameters:	fieldname, stream, filename, encoding, mimetype
 * Description:	File attachment processing function  
 */				  
			req.busboy.on('file', function(fieldname, stream, filename, encoding, mimetype) {
				
				var d = new TempData(); // This is local to the post requests; the field processing cannot see it	
								
				d.file = { // File return data type
					file_name: "",
					temp_file_name: "",
					file_encoding: "",	
					extension: "",
					jsonData: "",
					file_data: "",
					chunks: [],
					topojson: "",
					topojson_stderr: "",
					file_size: 0,
					transfer_time: '',
					uncompress_time: undefined,
					uncompress_size: undefined,
					lstart: ''
				};

				// This will need a mutex if > 1 thread is being processed at the same time
				response.no_files++;	// Increment file counter
				d.no_files=response.no_files; // Local copy
				
				d.file.file_name = filename;
				d.file.temp_file_name = os.tmpdir()  + "/" + filename;
				d.file.file_encoding=req.get('Content-Encoding');
				d.file.extension = filename.split('.').pop();
				d.file.lstart=new Date().getTime();
				
				if (!d.file.file_encoding) {
					if (d.file.extension === "gz") {
							d.file.file_encoding="gzip";
					}
					else if (d.file.extension === "lz77") {
							d.file.file_encoding="zlib";
					}
				}
	
/*
 * Function: 	req.busboy.on('file').stream.on:('data') callback function
 * Parameters:	None
 * Description: Data processor. Push data onto d.file.chunks[] array. Binary safe.
 */			
				stream.on('data', function(data) {
					d.file.chunks.push(data);  
				});

/*
 * Function: 	req.busboy.on('file').stream.on:('end') callback function
 * Parameters:	None
 * Description: EOF processor. Concatenate d.file.chunks[] array, uncompress if needed.
 */
				stream.on('end', function() {
		
					var buf=Buffer.concat(d.file.chunks); // Safe binary concat
					d.file.file_size=buf.length;
					var end = new Date().getTime();
					d.file.transfer_time=(end - d.file.lstart)/1000; // in S	
					
					d.file.file_data="";
					var lstart = new Date().getTime();
					if (d.file.file_encoding === "gzip") {
						try {
							d.file.file_data=zlib.gunzipSync(buf);
						}
						catch (e) {
							msg="FAIL! File [" + d.no_files + "]: " + d.file.file_name + "; extension: " + 
								d.file.extension + "; file_encoding: " + d.file.file_encoding + " gunzip exception";
							response.no_files=d.no_files;			// Add number of files process to response
							response.fields=ofields;				// Add return fields	
							httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('file').stream.on:('end')", 
								rifLog, 500, req, res, msg, e, response);									
						}	
						end = new Date().getTime();		
						d.file.uncompress_time=(end - lstart)/1000; // in S		
						d.file.uncompress_size=d.file.file_data.length;								
						rifLog.rifLog2(__file, __line, "req.busboy.on('file').stream.on:('end')", 
							"File [" + d.no_files + "]: " + d.file.file_name + "; encoding: " +
							d.file.file_encoding + "; zlib.gunzip(): " + d.file.file_data.length + 
							"; from buf: " + buf.length, req); 
					}	
					else if (d.file.file_encoding === "zlib") {	
						try {
							d.file.file_data=zlib.inflateSync(buf);
						}
						catch (e) {
							msg="FAIL! File [" + d.no_files + "]: " + d.file.file_name + "; extension: " + 
								d.file.extension + "; file_encoding: " + d.file.file_encoding + " inflate exception";
							response.no_files=d.no_files;			// Add number of files process to response
							response.fields=ofields;				// Add return fields	
							httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('file').stream.on:('end')", 
								rifLog, 500, req, res, msg, e, response);
							return;											
						}
						end = new Date().getTime();	
						d.file.uncompress_time=(end - lstart)/1000; // in S		
						d.file.uncompress_size=d.file.file_data.length;		
						rifLog.rifLog2(__file, __line, "req.busboy.on('file').stream.on:('end')", 
							"File [" + d.no_files + "]: " + d.file.file_name + "; encoding: " +
							d.file.file_encoding + "; zlib.inflate(): " + d.file.file_data.length + 
							"; from buf: " + buf.length, req); 
					}
					else if (d.file.file_encoding === "zip") {
						msg="FAIL! File [" + d.no_files + "]: " + d.file.file_name + "; extension: " + 
								d.file.extension + "; file_encoding: " + d.file.file_encoding + " not supported";
						response.no_files=d.no_files;			// Add number of files process to response
						response.fields=ofields;				// Add return fields	
						httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('file').stream.on:('end')", 
							rifLog, 500, req, res, msg, undefined, response);
						return;							
					}
					else {
						d.file.file_data=buf;
						rifLog.rifLog2(__file, __line, "req.busboy.on('file').stream.on:('end')", 			
							"File [" + d.no_files + "]: " + d.file.file_name + "; encoding: " +
							"; uncompressed data: " + d.file.file_data.length, req); 												
					}
					
					d_files.d_list[d.no_files-1] = d;										
				}); // End of EOF processor
					
			}); // End of file attachment processing function: req.busboy.on('file')
			  
/*
 * Function: 	req.busboy.on('field') callback function
 * Parameters:	fieldname, value, fieldnameTruncated, valTruncated
 * Description:	Field processing function; fields supported  
 *
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
 *			 	verbose: 	Set Topojson.Topology() option if true. Produces debug returned as part of reponse.message
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
			req.busboy.on('field', function(fieldname, val, fieldnameTruncated, valTruncated) {
				var text="\nField: " + fieldname + "[" + val + "]; ";
				
				// Handle truncation
				if (fieldnameTruncated) {
					text+="\FIELD PROCESSING ERROR! field truncated";
					response.field_errors++;
				}
				if (valTruncated) {
					text+="\FIELD PROCESSING ERROR! value truncated";
					response.field_errors++;
				}
				
				// Process fields
				if (fieldname == 'zoomLevel') {
				   options.quantization = getQuantization(val);
				   text+="Quantization set to: " + options.quantization;
				   ofields["quantization"]=options.quantization;
				}
				else if (fieldname == 'projection') {
				   options.projection = val;
				   text+="Projection set to: " + options.projection;
				   ofields["projection"]=options.projection;
				}
				else if ((fieldname == 'verbose')&&(val == 'true')) {
					options.verbose = true;
					text+="verbose mode enabled";
					ofields[fieldname]="true";
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
							if (options.id) {
								rifLog.rifLog2(__file, __line, "req.busboy.on('field')", msg, req);	
								options.id = undefined; // Prevent this section running again!	
								response.message = response.message + "\n" + msg;
							}
						}
						else {
							return d.properties[ofields[fieldname]];
						}
	//					response.message = response.message + "\nCall myId() for id field: " + ofields[fieldname] + 
	//						"; value: " + d.properties[ofields[fieldname]];									
					}						
					options.id = ofields.myId;				
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
									if (options["property-transform"]) {
										rifLog.rifLog2(__file, __line, "req.busboy.on('field')", msg, req);	
										options["property-transform"] = undefined; // Prevent this section running again!	
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
						options["property-transform"] = ofields.myPropertyTransform;	
					}
					catch (e) {
						response.field_errors++;
						msg="FIELD PROCESSING ERROR! field [" + fieldname + "]: " + val + "; invalid array exception";
						response.message = response.message + "\n" + msg;
						rifLog.rifLog2(__file, __line, "req.busboy.on('field')", msg, req);							
					}
				}
				else {
					ofields[fieldname]=val;				
				}	
				response.message = response.message + text;
			 }); // End of field processing function

/*
 * Function: 	req.busboy.on('finish') callback function
 * Parameters:	None
 * Description:	End of request - complete response		  
 */ 
			req.busboy.on('finish', function() {
				var msg;
				
				for (i = 0; i < response.no_files; i++) {	
					d=d_files.d_list[i];
					if (!d) { // File could not be processed, httpErrorResponse.httpErrorResponse() already processed
//						msg="FAIL! File [" + (i+1) + "/?]: entry not found, no file list";
//						response.no_files=0;					// Add number of files process to response
//						response.fields=ofields;				// Add return fields	
//						httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('finish')", rifLog, 500, req, res, msg, undefined, response);
						return;							
					}
					else if (!d.file) {
						msg="FAIL! File [" + (i+1) + "/" + d.no_files + "]: object not found in list";
						response.no_files=d.no_files;			// Add number of files process to response
						response.fields=ofields;				// Add return fields	
						httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('finish')", 
							rifLog, 500, req, res, msg, undefined, response);
						return;			
					}
					else if (d.file.file_data.length > 0) {
						d=_process_json(d, ofields, options, stderr, req, res, response);	
						if (!d) {
							return; // _process_json() has emitted the error
						}
					}	
					else {
						msg="FAIL! File [" + (i+1) + "/" + d.no_files + "]: " + d.file.file_name + "; extension: " + 
								d.file.extension + "; file size is zero";
		
						response.no_files=d.no_files;			// Add number of files process to response
						response.fields=ofields;				// Add return fields							
						httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('finish')", 
							rifLog, 500, req, res, msg, undefined, response);
						return;
					}	
				}
				if (!ofields["my_reference"]) {
					msg="[No my_reference] Processed: " + response.no_files + " files; debug message:\n" + response.message
				}
				else {
					msg="[my_reference: " + ofields["my_reference"] + "] Processed: " + response.no_files + " files; debug message:\n" + response.message
				}
/*
 * Response object - no errors:
 *                    
 * no_files: 		Numeric, number of files    
 * field_errors: 	Number of errors in processing fields
 * file_list: 		Array file objects:
 *						file_name: File name
 *						topojson: TopoJSON created from file geoJSON,
 *						topojson_stderr: Debug from TopoJSON module,
 *						topojson_runtime: Time to convert geoJSON to topoJSON (S),
 *						file_size: Transferred file size in bytes,
 *						transfer_time: Time to transfer file (S),
 *						uncompress_time: Time to uncompress file (S)/undefined if file not compressed,
 *						uncompress_size: Size of uncompressed file in bytes
 * message: 		Processing messages, including debug from topoJSON               
 * fields: 			Array of fields; includes all from request plus any additional fields set as a result of processing  
 */ 
				response.fields=ofields;				// Add return fields	
				if (response.field_errors == 0) { // OK
					rifLog.rifLog2(__file, __line, "req.busboy.on:('finish')", msg, req);					
					var output = JSON.stringify(response);// Convert output response to JSON 
	// Need to test res was not finished by an expection to avoid "write after end" errors			
					res.write(output);                  // Write output  
					res.end();	

//					console.error(util.inspect(req));
//					console.error(JSON.stringify(req.headers, null, 4));
				}
				else {
					msg = "FAIL! Field processing ERRORS! " + response.field_errors + "\n" + msg;
					httpErrorResponse.httpErrorResponse(__file, __line, "req.busboy.on('finish')", 
						rifLog, 500, req, res, msg, undefined, response);				  
				}		

			});

			req.pipe(req.busboy); // Pipe request stream to busboy form data handler
			  
		} // End of post method
		else {								// All other methods are errors
			var msg="ERROR! "+ req.method + " Requests not allowed; please see: " + 
				"https://github.com/smallAreaHealthStatisticsUnit/rapidInquiryFacility/blob/master/rifNodeServices/readme.md Node Web Services API for RIF 4.0 documentation for help";
			httpErrorResponse.httpErrorResponse(__file, __line, "exports.convert", 
				rifLog, 405, req, res, msg);		
			return;		  
		}
		
	} catch (e) {                            // Catch syntax errors
		var msg="General processing ERROR!";				  
		httpErrorResponse.httpErrorResponse(__file, __line, "exports.convert catch()", rifLog, 500, req, res, msg, e);		
		return;
	}
	  
};