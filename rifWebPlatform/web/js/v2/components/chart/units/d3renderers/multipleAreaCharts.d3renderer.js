RIF.chart.multipleAreaCharts.d3renderer = ( function( settings, rSet, max ) {
  //Will sort it later
  var margin = settings.margin,
    width = settings.dimensions.width(),
    height = settings.dimensions.height(),
    contextHeight = 50,
    contextWidth = width * .5,
    el = settings.element;

  var xScales = {},
    yScales = {};

  var dataSets = {};

  var svg = d3.select( "#" + el ).append( "svg" )
    .attr( "width", width )
    .attr( "height", ( height + margin.top + margin.bottom ) )
    .attr( "class", "areaCharts" );


  var rSetCount = rSet.length,
    chartHeight = ( height / rSetCount ) - ( margin.top + margin.bottom ) - ( Math.log( ( height * height ) ) ),
    maxDataPoint = max;


  return function Chart( options ) {

    this.width = width - 15;
    this.height = chartHeight;
    this.maxDataPoint = maxDataPoint;
    this.id = options.id;
    this.name = options.name;
    this.margin = margin;

    var localName = this.name;

    dataSets[ localName ] = options.data;

    var xS = d3.scale.ordinal()
      .domain( options.data.map( function( d ) {
        return d.gid;
      } ) )
      .rangeBands( [ 0, this.width ] );

    var yS = d3.scale.linear()
      .range( [ this.height, 0 ] )
      .domain( [ 0, this.maxDataPoint ] );


    var linename = this.name + "_line";

    xScales[ linename ] = xS;
    yScales[ linename ] = yS;


    this.area = d3.svg.area()
      .interpolate( "basis" )
      .x( function( d ) {
        return xS( +d.gid.toString() );
      } )
      .y0( function( d ) {
        if ( d[ localName ] < 1 ) {
          return yS( d[ localName ] );
        } else {
          return yS( 1 )
        }

      } )
      .y1( function( d ) {
        if ( d[ localName ] < 1 ) {
          return yS( 1 );
        } else {
          return yS( d[ localName ] )
        }
      } );

    /*this.area2 = d3.svg.area()
      .interpolate( "monotone" )
      .x( function( d ) {
        return xS( +d.gid.toString() );
      } )
      .y0( function( d ) {
        var f = d[ localName ] - 0.1;
        if ( f < 1 ) {
          return yS( f );
        } else {
          return yS( 1 )
        }

      } )
      .y1( function( d ) {
        var f = d[ localName ] - 0.1;
        if ( f < 1 ) {
          return yS( 1 );
        } else {
          return yS( f )
        }
      } );*/

    /*
     
	 This isn't required - it simply creates a mask. If this wasn't here,
	 when we zoom/panned, we'd see the chart go off to the left under the y-axis 
	
    svg.append( "defs" ).append( "clipPath" )
      .attr( "id", "clip-" + this.id )
      .append( "rect" )
      .attr( "width", this.width )
      .attr( "height", this.height );
	  
	*/

    /*
		Assign it a class so we can assign a fill color
		And position it on the page
	*/

    this.chartContainer = svg.append( "g" )
      .attr( 'class', this.name.toLowerCase() )
      .attr( "transform", "translate(" + this.margin.left + "," + ( this.margin.top + ( this.height * this.id ) + ( 10 * this.id ) ) + ")" );

    /* We've created everything, let's actually add it to the page */
    this.chartContainer.append( "path" )
      .data( [ options.data ] )
      .attr( "class", "chart unadj " + this.name.toLowerCase() )
      .attr( "clip-path", "url(#clip-" + this.id + ")" )
      .attr( "d", this.area );

    /* this.chartContainer.append( "path" )
      .data( [ this.chartData ] )
      .attr( "class", "chart adj " + this.name.toLowerCase() )
      .attr( "clip-path", "url(#clip-" + this.id + ")" )
      .attr( "d", this.area2 );*/


    var mouseclick = function( d ) {

      var xy = d3.mouse( this ),
        xPos = xy[ 0 ],
        yPos = xy[ 1 ],
        leftEdges = xS.range(),
        width = xS.rangeBand(),
        j;


      for ( j = 0; xPos > ( leftEdges[ j ] + width ); j++ ) {}
      //do nothing, just increment j until case fails

      var xOrdinal = xS.domain()[ j ],
        yValues = {},
        dataLength = options.data.length - 1;


      while ( dataLength-- ) {
        for ( var set in dataSets ) {
          if ( dataSets[ set ][ dataLength ].gid === xOrdinal ) {
            yValue = options.data[ dataLength ][ localName ];
            yValues[ set ] = yValue;
            break;
          };
        };
      };

      svg.selectAll( ".lineHover" )
        .attr( "transform", function() {
          var x = xScales[ this.id ]( xOrdinal );
          //console.log(dataSets[options.name][])
          return "translate(" + x + "," + 0 + ")";
        } )

      svg.selectAll( "text.areaValue" )
        .text( function() {
          return xOrdinal + ":" + yValues[ this.id ]
        } );

    };

    this.chartContainer.append( "rect" )
      .attr( "class", "overlayHover" )
      .attr( "width", width )
      .attr( "height", chartHeight )
      .on( "mousemove", mouseclick );

    /* Highlighter */
    var highlight = this.chartContainer.append( "line" )
      .attr( "class", "lineHover" )
      .attr( "x1", 0 )
      .attr( "y1", 0 )
      .attr( "x2", 0 )
      .attr( "y2", chartHeight )
      .attr( "height", 2 )
      .attr( "height", chartHeight )
      .attr( "id", linename );

    //this.chartContainer.append( lineHover );

    //this.xAxisTop = d3.svg.axis().scale( xS ).orient( "bottom" );
    //this.xAxisBottom = d3.svg.axis().scale( xS ).orient( "top" );
    this.yAxis = d3.svg.axis().scale( yS ).orient( "left" ).tickValues( [ 0, 1, this.maxDataPoint ] );

    this.chartContainer.append( "g" )
      .attr( "class", "y axis" )
      .attr( "transform", "translate(0,0)" )
      .call( this.yAxis );

    this.chartContainer.append( "text" )
      .attr( "class", "country-title" )
      .attr( "transform", "translate(10,20)" )
      .text( this.name );

    this.chartContainer.append( "text" )
      .attr( "class", "areaValue" )
      .attr( "id", localName )
      .attr( "transform", "translate(10,32)" )
      .text( "0.00" );



  };



  /*Chart.prototype.showOnly = function( b ) {
    this.xScale.domain( b );
    this.chartContainer.select( "path" ).data( [ this.chartData ] ).attr( "d", this.area );
    this.chartContainer.select( ".x.axis.top" ).call( this.xAxisTop );
    this.chartContainer.select( ".x.axis.bottom" ).call( this.xAxisBottom );
  }*/
} );