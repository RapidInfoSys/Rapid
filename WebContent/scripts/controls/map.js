/**
 * GeoTools javascript coordinate transformations
 * http://files.dixo.net/geotools.html
 *
 * This file copyright (c)2005 Paul Dixon (paul@elphin.com)
 *

 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 

 * --------------------------------------------------------------------------- 
 * 
 * Credits

 *

 * The algorithm used by the script for WGS84-OSGB36 conversions is derived 
 * from an OSGB spreadsheet (www.gps.gov.uk) with permission. This has been
 * adapted into PHP by Ian Harris, and Irish added by Barry Hunter. Conversion
 * accuracy is in the order of 7m for 90% of Great Britain, and should be 
 * be similar to the conversion made by a typical GPSr
 *

 * See accompanying documentation for more information
 * http://www.nearby.org.uk/tests/GeoTools2.html
 */
function GT_OSGB()
{this.northings=0;this.eastings=0;this.status="Undefined";}
GT_OSGB.prefixes=new Array(new Array("SV","SW","SX","SY","SZ","TV","TW"),new Array("SQ","SR","SS","ST","SU","TQ","TR"),new Array("SL","SM","SN","SO","SP","TL","TM"),new Array("SF","SG","SH","SJ","SK","TF","TG"),new Array("SA","SB","SC","SD","SE","TA","TB"),new Array("NV","NW","NX","NY","NZ","OV","OW"),new Array("NQ","NR","NS","NT","NU","OQ","OR"),new Array("NL","NM","NN","NO","NP","OL","OM"),new Array("NF","NG","NH","NJ","NK","OF","OG"),new Array("NA","NB","NC","ND","NE","OA","OB"),new Array("HV","HW","HX","HY","HZ","JV","JW"),new Array("HQ","HR","HS","HT","HU","JQ","JR"),new Array("HL","HM","HN","HO","HP","JL","JM"));GT_OSGB.prototype.setGridCoordinates=function(eastings,northings)
{this.northings=northings;this.eastings=eastings;this.status="OK";}
GT_OSGB.prototype.setError=function(msg)
{this.status=msg;}
GT_OSGB.prototype._zeropad=function(num,len)
{var str=new String(num);while(str.length<len)
{str='0'+str;}
return str;}
GT_OSGB.prototype.getGridRef=function(precision)
{if(precision<0)
precision=0;if(precision>5)
precision=5;var e="";var n="";if(precision>0)
{var y=Math.floor(this.northings/100000);var x=Math.floor(this.eastings/100000);var e=Math.round(this.eastings%100000);var n=Math.round(this.northings%100000);var div=(5-precision);e=Math.round(e/Math.pow(10,div));n=Math.round(n/Math.pow(10,div));}
var prefix=GT_OSGB.prefixes[y][x];return prefix+" "+this._zeropad(e,precision)+" "+this._zeropad(n,precision);}
GT_OSGB.prototype.parseGridRef=function(landranger)
{var ok=false;this.northings=0;this.eastings=0;var precision;for(precision=5;precision>=1;precision--)
{var pattern=new RegExp("^([A-Z]{2})\\s*(\\d{"+precision+"})\\s*(\\d{"+precision+"})$","i")
var gridRef=landranger.match(pattern);if(gridRef)
{var gridSheet=gridRef[1];var gridEast=0;var gridNorth=0;if(precision>0)
{var mult=Math.pow(10,5-precision);gridEast=parseInt(gridRef[2],10)*mult;gridNorth=parseInt(gridRef[3],10)*mult;}
var x,y;search:for(y=0;y<GT_OSGB.prefixes.length;y++)
{for(x=0;x<GT_OSGB.prefixes[y].length;x++)
if(GT_OSGB.prefixes[y][x]==gridSheet){this.eastings=(x*100000)+gridEast;this.northings=(y*100000)+gridNorth;ok=true;break search;}}}}
return ok;}
GT_OSGB.prototype.getWGS84=function()
{var height=0;var lat1=GT_Math.E_N_to_Lat(this.eastings,this.northings,6377563.396,6356256.910,400000,-100000,0.999601272,49.00000,-2.00000);var lon1=GT_Math.E_N_to_Long(this.eastings,this.northings,6377563.396,6356256.910,400000,-100000,0.999601272,49.00000,-2.00000);var x1=GT_Math.Lat_Long_H_to_X(lat1,lon1,height,6377563.396,6356256.910);var y1=GT_Math.Lat_Long_H_to_Y(lat1,lon1,height,6377563.396,6356256.910);var z1=GT_Math.Lat_H_to_Z(lat1,height,6377563.396,6356256.910);var x2=GT_Math.Helmert_X(x1,y1,z1,446.448,0.2470,0.8421,-20.4894);var y2=GT_Math.Helmert_Y(x1,y1,z1,-125.157,0.1502,0.8421,-20.4894);var z2=GT_Math.Helmert_Z(x1,y1,z1,542.060,0.1502,0.2470,-20.4894);var latitude=GT_Math.XYZ_to_Lat(x2,y2,z2,6378137.000,6356752.313);var longitude=GT_Math.XYZ_to_Long(x2,y2);var wgs84=new GT_WGS84();wgs84.setDegrees(latitude,longitude);return wgs84;}
function GT_Irish()
{this.northings=0;this.eastings=0;this.status="Undefined";}
GT_Irish.prefixes=new Array(new Array("V","Q","L","F","A"),new Array("W","R","M","G","B"),new Array("X","S","N","H","C"),new Array("Y","T","O","J","D"),new Array("Z","U","P","K","E"));GT_Irish.prototype.setGridCoordinates=function(eastings,northings)
{this.northings=northings;this.eastings=eastings;this.status="OK";}
GT_Irish.prototype.setError=function(msg)
{this.status=msg;}
GT_Irish.prototype._zeropad=function(num,len)
{var str=new String(num);while(str.length<len)
{str='0'+str;}
return str;}
GT_Irish.prototype.getGridRef=function(precision)
{if(precision<0)
precision=0;if(precision>5)
precision=5;var e="";var n="";if(precision>0)
{var y=Math.floor(this.northings/100000);var x=Math.floor(this.eastings/100000);var e=Math.round(this.eastings%100000);var n=Math.round(this.northings%100000);var div=(5-precision);e=Math.round(e/Math.pow(10,div));n=Math.round(n/Math.pow(10,div));}
var prefix=GT_Irish.prefixes[x][y];return prefix+" "+this._zeropad(e,precision)+" "+this._zeropad(n,precision);}
GT_Irish.prototype.parseGridRef=function(landranger)
{var ok=false;this.northings=0;this.eastings=0;var precision;for(precision=5;precision>=1;precision--)
{var pattern=new RegExp("^([A-Z]{1})\\s*(\\d{"+precision+"})\\s*(\\d{"+precision+"})$","i")
var gridRef=landranger.match(pattern);if(gridRef)
{var gridSheet=gridRef[1];var gridEast=0;var gridNorth=0;if(precision>0)
{var mult=Math.pow(10,5-precision);gridEast=parseInt(gridRef[2],10)*mult;gridNorth=parseInt(gridRef[3],10)*mult;}
var x,y;search:for(x=0;x<GT_Irish.prefixes.length;x++)
{for(y=0;y<GT_Irish.prefixes[x].length;y++)
if(GT_Irish.prefixes[x][y]==gridSheet){this.eastings=(x*100000)+gridEast;this.northings=(y*100000)+gridNorth;ok=true;break search;}}}}
return ok;}
GT_Irish.prototype.getWGS84=function(uselevel2)
{var height=0;if(uselevel2){e=this.eastings;n=this.northings;}else{e=this.eastings-49;n=this.northings+23.4;}
var lat1=GT_Math.E_N_to_Lat(e,n,6377340.189,6356034.447,200000,250000,1.000035,53.50000,-8.00000);var lon1=GT_Math.E_N_to_Long(e,n,6377340.189,6356034.447,200000,250000,1.000035,53.50000,-8.00000);var wgs84=new GT_WGS84();if(uselevel2){var x1=GT_Math.Lat_Long_H_to_X(lat1,lon1,height,6377340.189,6356034.447);var y1=GT_Math.Lat_Long_H_to_Y(lat1,lon1,height,6377340.189,6356034.447);var z1=GT_Math.Lat_H_to_Z(lat1,height,6377340.189,6356034.447);var x2=GT_Math.Helmert_X(x1,y1,z1,482.53,0.214,0.631,8.15);var y2=GT_Math.Helmert_Y(x1,y1,z1,-130.596,1.042,0.631,8.15);var z2=GT_Math.Helmert_Z(x1,y1,z1,564.557,1.042,0.214,8.15);var latitude=GT_Math.XYZ_to_Lat(x2,y2,z2,6378137.000,6356752.313);var longitude=GT_Math.XYZ_to_Long(x2,y2);wgs84.setDegrees(latitude,longitude);}else{wgs84.setDegrees(lat1,lon1);}
return wgs84;}
function GT_WGS84()
{this.latitude=0;this.longitude=0;}
GT_WGS84.prototype.setDegrees=function(latitude,longitude)
{this.latitude=latitude;this.longitude=longitude;}
GT_WGS84.prototype.parseString=function(text)
{var ok=false;var str=new String(text);var pattern=/([ns])\s*(\d+)[�\s]+(\d+\.\d+)\s+([we])\s*(\d+)[�\s]+(\d+\.\d+)/i;var matches=str.match(pattern);if(matches)
{ok=true;var latsign=(matches[1]=='s'||matches[1]=='S')?-1:1;var longsign=(matches[4]=='w'||matches[4]=='W')?-1:1;var d1=parseFloat(matches[2]);var m1=parseFloat(matches[3]);var d2=parseFloat(matches[5]);var m2=parseFloat(matches[6]);this.latitude=latsign*(d1+(m1/60.0));this.longitude=longsign*(d2+(m2/60.0));}
return ok;}
GT_WGS84.prototype.isGreatBritain=function()
{return this.latitude>49&&this.latitude<62&&this.longitude>-9.5&&this.longitude<2.3;}
GT_WGS84.prototype.isIreland=function()
{return this.latitude>51.2&&this.latitude<55.73&&this.longitude>-12.2&&this.longitude<-4.8;}
GT_WGS84.prototype.getIrish=function(uselevel2)
{var irish=new GT_Irish();if(this.isIreland())
{var height=0;if(uselevel2){var x1=GT_Math.Lat_Long_H_to_X(this.latitude,this.longitude,height,6378137.00,6356752.313);var y1=GT_Math.Lat_Long_H_to_Y(this.latitude,this.longitude,height,6378137.00,6356752.313);var z1=GT_Math.Lat_H_to_Z(this.latitude,height,6378137.00,6356752.313);var x2=GT_Math.Helmert_X(x1,y1,z1,-482.53,-0.214,-0.631,-8.15);var y2=GT_Math.Helmert_Y(x1,y1,z1,130.596,-1.042,-0.631,-8.15);var z2=GT_Math.Helmert_Z(x1,y1,z1,-564.557,-1.042,-0.214,-8.15);var latitude2=GT_Math.XYZ_to_Lat(x2,y2,z2,6377340.189,6356034.447);var longitude2=GT_Math.XYZ_to_Long(x2,y2);}else{var latitude2=this.latitude;var longitude2=this.longitude;}
var e=GT_Math.Lat_Long_to_East(latitude2,longitude2,6377340.189,6356034.447,200000,1.000035,53.50000,-8.00000);var n=GT_Math.Lat_Long_to_North(latitude2,longitude2,6377340.189,6356034.447,200000,250000,1.000035,53.50000,-8.00000);if(!uselevel2){e=e+49;n=n-23.4;}
irish.setGridCoordinates(Math.round(e),Math.round(n));}
else
{irish.setError("Coordinate not within Ireland");}
return irish;}
GT_WGS84.prototype.getOSGB=function(uselevel2)
{var osgb=new GT_OSGB();if(this.isGreatBritain())
{var height=0;var x1=GT_Math.Lat_Long_H_to_X(this.latitude,this.longitude,height,6378137.00,6356752.313);var y1=GT_Math.Lat_Long_H_to_Y(this.latitude,this.longitude,height,6378137.00,6356752.313);var z1=GT_Math.Lat_H_to_Z(this.latitude,height,6378137.00,6356752.313);var x2=GT_Math.Helmert_X(x1,y1,z1,-446.448,-0.2470,-0.8421,20.4894);var y2=GT_Math.Helmert_Y(x1,y1,z1,125.157,-0.1502,-0.8421,20.4894);var z2=GT_Math.Helmert_Z(x1,y1,z1,-542.060,-0.1502,-0.2470,20.4894);var latitude2=GT_Math.XYZ_to_Lat(x2,y2,z2,6377563.396,6356256.910);var longitude2=GT_Math.XYZ_to_Long(x2,y2);var e=GT_Math.Lat_Long_to_East(latitude2,longitude2,6377563.396,6356256.910,400000,0.999601272,49.00000,-2.00000);var n=GT_Math.Lat_Long_to_North(latitude2,longitude2,6377563.396,6356256.910,400000,-100000,0.999601272,49.00000,-2.00000);osgb.setGridCoordinates(Math.round(e),Math.round(n));}
else
{osgb.setError("Coordinate not within Great Britain");}
return osgb;}
function GT_Math()
{}
GT_Math.E_N_to_Lat=function(East,North,a,b,e0,n0,f0,PHI0,LAM0)
{var Pi=3.14159265358979;var RadPHI0=PHI0*(Pi/180);var RadLAM0=LAM0*(Pi/180);var af0=a*f0;var bf0=b*f0;var e2=(Math.pow(af0,2)-Math.pow(bf0,2))/Math.pow(af0,2);var n=(af0-bf0)/(af0+bf0);var Et=East-e0;var PHId=GT_Math.InitialLat(North,n0,af0,RadPHI0,n,bf0);var nu=af0/(Math.sqrt(1-(e2*(Math.pow(Math.sin(PHId),2)))));var rho=(nu*(1-e2))/(1-(e2*Math.pow(Math.sin(PHId),2)));var eta2=(nu/rho)-1;var VII=(Math.tan(PHId))/(2*rho*nu);var VIII=((Math.tan(PHId))/(24*rho*Math.pow(nu,3)))*(5+(3*(Math.pow(Math.tan(PHId),2)))+eta2-(9*eta2*(Math.pow(Math.tan(PHId),2))));var IX=((Math.tan(PHId))/(720*rho*Math.pow(nu,5)))*(61+(90*((Math.tan(PHId))^2))+(45*(Math.pow(Math.tan(PHId),4))));var E_N_to_Lat=(180/Pi)*(PHId-(Math.pow(Et,2)*VII)+(Math.pow(Et,4)*VIII)-((Et^6)*IX));return(E_N_to_Lat);}
GT_Math.E_N_to_Long=function(East,North,a,b,e0,n0,f0,PHI0,LAM0)
{var Pi=3.14159265358979;var RadPHI0=PHI0*(Pi/180);var RadLAM0=LAM0*(Pi/180);var af0=a*f0;var bf0=b*f0;var e2=(Math.pow(af0,2)-Math.pow(bf0,2))/Math.pow(af0,2);var n=(af0-bf0)/(af0+bf0);var Et=East-e0;var PHId=GT_Math.InitialLat(North,n0,af0,RadPHI0,n,bf0);var nu=af0/(Math.sqrt(1-(e2*(Math.pow(Math.sin(PHId),2)))));var rho=(nu*(1-e2))/(1-(e2*Math.pow(Math.sin(PHId),2)));var eta2=(nu/rho)-1;var X=(Math.pow(Math.cos(PHId),-1))/nu;var XI=((Math.pow(Math.cos(PHId),-1))/(6*Math.pow(nu,3)))*((nu/rho)+(2*(Math.pow(Math.tan(PHId),2))));var XII=((Math.pow(Math.cos(PHId),-1))/(120*Math.pow(nu,5)))*(5+(28*(Math.pow(Math.tan(PHId),2)))+(24*(Math.pow(Math.tan(PHId),4))));var XIIA=((Math.pow(Math.cos(PHId),-1))/(5040*Math.pow(nu,7)))*(61+(662*(Math.pow(Math.tan(PHId),2)))+(1320*(Math.pow(Math.tan(PHId),4)))+(720*(Math.pow(Math.tan(PHId),6))));var E_N_to_Long=(180/Pi)*(RadLAM0+(Et*X)-(Math.pow(Et,3)*XI)+(Math.pow(Et,5)*XII)-(Math.pow(Et,7)*XIIA));return E_N_to_Long;}
GT_Math.InitialLat=function(North,n0,afo,PHI0,n,bfo)
{var PHI1=((North-n0)/afo)+PHI0;var M=GT_Math.Marc(bfo,n,PHI0,PHI1);var PHI2=((North-n0-M)/afo)+PHI1;while(Math.abs(North-n0-M)>0.00001)
{PHI2=((North-n0-M)/afo)+PHI1;M=GT_Math.Marc(bfo,n,PHI0,PHI2);PHI1=PHI2;}
return PHI2;}
GT_Math.Lat_Long_H_to_X=function(PHI,LAM,H,a,b)
{var Pi=3.14159265358979;var RadPHI=PHI*(Pi/180);var RadLAM=LAM*(Pi/180);var e2=(Math.pow(a,2)-Math.pow(b,2))/Math.pow(a,2);var V=a/(Math.sqrt(1-(e2*(Math.pow(Math.sin(RadPHI),2)))));return(V+H)*(Math.cos(RadPHI))*(Math.cos(RadLAM));}
GT_Math.Lat_Long_H_to_Y=function(PHI,LAM,H,a,b)
{var Pi=3.14159265358979;var RadPHI=PHI*(Pi/180);var RadLAM=LAM*(Pi/180);var e2=(Math.pow(a,2)-Math.pow(b,2))/Math.pow(a,2);var V=a/(Math.sqrt(1-(e2*(Math.pow(Math.sin(RadPHI),2)))));return(V+H)*(Math.cos(RadPHI))*(Math.sin(RadLAM));}
GT_Math.Lat_H_to_Z=function(PHI,H,a,b)
{var Pi=3.14159265358979;var RadPHI=PHI*(Pi/180);var e2=(Math.pow(a,2)-Math.pow(b,2))/Math.pow(a,2);var V=a/(Math.sqrt(1-(e2*(Math.pow(Math.sin(RadPHI),2)))));return((V*(1-e2))+H)*(Math.sin(RadPHI));}
GT_Math.Helmert_X=function(X,Y,Z,DX,Y_Rot,Z_Rot,s)
{var Pi=3.14159265358979;var sfactor=s*0.000001;var RadY_Rot=(Y_Rot/3600)*(Pi/180);var RadZ_Rot=(Z_Rot/3600)*(Pi/180);return(X+(X*sfactor)-(Y*RadZ_Rot)+(Z*RadY_Rot)+DX);}
GT_Math.Helmert_Y=function(X,Y,Z,DY,X_Rot,Z_Rot,s)
{var Pi=3.14159265358979;var sfactor=s*0.000001;var RadX_Rot=(X_Rot/3600)*(Pi/180);var RadZ_Rot=(Z_Rot/3600)*(Pi/180);return(X*RadZ_Rot)+Y+(Y*sfactor)-(Z*RadX_Rot)+DY;}
GT_Math.Helmert_Z=function(X,Y,Z,DZ,X_Rot,Y_Rot,s)
{var Pi=3.14159265358979;var sfactor=s*0.000001;var RadX_Rot=(X_Rot/3600)*(Pi/180);var RadY_Rot=(Y_Rot/3600)*(Pi/180);return(-1*X*RadY_Rot)+(Y*RadX_Rot)+Z+(Z*sfactor)+DZ;}
GT_Math.XYZ_to_Lat=function(X,Y,Z,a,b)
{var RootXYSqr=Math.sqrt(Math.pow(X,2)+Math.pow(Y,2));var e2=(Math.pow(a,2)-Math.pow(b,2))/Math.pow(a,2);var PHI1=Math.atan2(Z,(RootXYSqr*(1-e2)));var PHI=GT_Math.Iterate_XYZ_to_Lat(a,e2,PHI1,Z,RootXYSqr);var Pi=3.14159265358979;return PHI*(180/Pi);}
GT_Math.Iterate_XYZ_to_Lat=function(a,e2,PHI1,Z,RootXYSqr)
{var V=a/(Math.sqrt(1-(e2*Math.pow(Math.sin(PHI1),2))));var PHI2=Math.atan2((Z+(e2*V*(Math.sin(PHI1)))),RootXYSqr);while(Math.abs(PHI1-PHI2)>0.000000001){PHI1=PHI2;V=a/(Math.sqrt(1-(e2*Math.pow(Math.sin(PHI1),2))));PHI2=Math.atan2((Z+(e2*V*(Math.sin(PHI1)))),RootXYSqr);}
return PHI2;}
GT_Math.XYZ_to_Long=function(X,Y)
{var Pi=3.14159265358979;return Math.atan2(Y,X)*(180/Pi);}
GT_Math.Marc=function(bf0,n,PHI0,PHI)
{return bf0*(((1+n+((5/4)*Math.pow(n,2))+((5/4)*Math.pow(n,3)))*(PHI-PHI0))-(((3*n)+(3*Math.pow(n,2))+((21/8)*Math.pow(n,3)))*(Math.sin(PHI-PHI0))*(Math.cos(PHI+PHI0)))+((((15/8)*Math.pow(n,2))+((15/8)*Math.pow(n,3)))*(Math.sin(2*(PHI-PHI0)))*(Math.cos(2*(PHI+PHI0))))-(((35/24)*Math.pow(n,3))*(Math.sin(3*(PHI-PHI0)))*(Math.cos(3*(PHI+PHI0)))));}
GT_Math.Lat_Long_to_East=function(PHI,LAM,a,b,e0,f0,PHI0,LAM0)
{var Pi=3.14159265358979;var RadPHI=PHI*(Pi/180);var RadLAM=LAM*(Pi/180);var RadPHI0=PHI0*(Pi/180);var RadLAM0=LAM0*(Pi/180);var af0=a*f0;var bf0=b*f0;var e2=(Math.pow(af0,2)-Math.pow(bf0,2))/Math.pow(af0,2);var n=(af0-bf0)/(af0+bf0);var nu=af0/(Math.sqrt(1-(e2*Math.pow(Math.sin(RadPHI),2))));var rho=(nu*(1-e2))/(1-(e2*Math.pow(Math.sin(RadPHI),2)));var eta2=(nu/rho)-1;var p=RadLAM-RadLAM0;var IV=nu*(Math.cos(RadPHI));var V=(nu/6)*(Math.pow(Math.cos(RadPHI),3))*((nu/rho)-(Math.pow(Math.tan(RadPHI),2)));var VI=(nu/120)*(Math.pow(Math.cos(RadPHI),5))*(5-(18*(Math.pow(Math.tan(RadPHI),2)))+(Math.pow(Math.tan(RadPHI),4))+(14*eta2)-(58*(Math.pow(Math.tan(RadPHI),2))*eta2));return e0+(p*IV)+(Math.pow(p,3)*V)+(Math.pow(p,5)*VI);}
GT_Math.Lat_Long_to_North=function(PHI,LAM,a,b,e0,n0,f0,PHI0,LAM0)
{var Pi=3.14159265358979;var RadPHI=PHI*(Pi/180);var RadLAM=LAM*(Pi/180);var RadPHI0=PHI0*(Pi/180);var RadLAM0=LAM0*(Pi/180);var af0=a*f0;var bf0=b*f0;var e2=(Math.pow(af0,2)-Math.pow(bf0,2))/Math.pow(af0,2);var n=(af0-bf0)/(af0+bf0);var nu=af0/(Math.sqrt(1-(e2*Math.pow(Math.sin(RadPHI),2))));var rho=(nu*(1-e2))/(1-(e2*Math.pow(Math.sin(RadPHI),2)));var eta2=(nu/rho)-1;var p=RadLAM-RadLAM0;var M=GT_Math.Marc(bf0,n,RadPHI0,RadPHI);var I=M+n0;var II=(nu/2)*(Math.sin(RadPHI))*(Math.cos(RadPHI));var III=((nu/24)*(Math.sin(RadPHI))*(Math.pow(Math.cos(RadPHI),3)))*(5-(Math.pow(Math.tan(RadPHI),2))+(9*eta2));var IIIA=((nu/720)*(Math.sin(RadPHI))*(Math.pow(Math.cos(RadPHI),5)))*(61-(58*(Math.pow(Math.tan(RadPHI),2)))+(Math.pow(Math.tan(RadPHI),4)));return I+(Math.pow(p,2)*II)+(Math.pow(p,4)*III)+(Math.pow(p,6)*IIIA);}


/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */


/*

Copyright (C) 2019 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

// an array to manage the in-page maps	                
var _maps = [];	    
// an object of map details for use in the loaded call back
var _mapDetails = {};
// a global for the geo-coder (if required)
var _geocoder = null;

// dynamically load the map javascript with the given key
function loadMapJavaScript(id, details) {	
	// append loading script only if not there already
	if (!$("#mapJavaScript")[0])	{
		// inc loading controls
		_loadingControls ++;
		// assume no key provided
		var url = "https://maps.googleapis.com/maps/api/js?v=3&callback=loadedMapJavaScript"
		// if there was one
		if (details.key) url += "&key=" + details.key;
		// if there is a heat map add the visualization library
		if (details.heatmap) url += "&&libraries=visualization";
		// append to the head and start loading
		$("head").append("<script async defer id='mapJavaScript' type='text/javascript' src='" + url + "'></script>");
	}
	// retain the details for use in the loaded callback
	_mapDetails[id] = details;
}

// call back for once map JavaScript is loaded
function loadedMapJavaScript() {
	// dec loading controls
	_loadingControls --;
	// loop the stored details
	for (var id in _mapDetails) {
		// build or rebuild the map by id
		rebuildLoadedMap(id);			
	}
	// if all controls are loaded
	if (window["_loadingPages"] && _loadingControls < 1) {
		// loop any pages waiting to load
		for (var i in _loadingPages) {
			// get the page id
			var pageId = _loadingPages[i];
			// get any pageload method
			var pageload = window["Event_pageload_" + pageId];
			// call it if there was one
			if (pageload) pageload($.Event('pageload'));
		}
	}

}

function rebuildLoadedMap(id) {
	
	// if google has loaded - might not if offline
	if (window["google"] && window["google"].maps) {
	
		// get the details
		var details = _mapDetails[id];
	
		// make the zoom a number
		var zoom = parseInt(details.zoom);
		
		// assume we want a roadmap
		var mapTypeId =  google.maps.MapTypeId.ROADMAP;
		
		// update if one provided
		switch (details.mapType) {
			case ("R") :
				mapTypeId =  google.maps.MapTypeId.ROADMAP;
			break;
			case ("S") :
				mapTypeId =  google.maps.MapTypeId.SATELLITE;
			break;
		} 
		
		// create a map in our control object
		var map = new google.maps.Map($("#" + id)[0], {
		   	zoom: zoom,
			center: new google.maps.LatLng(details.lat, details.lng),
			mapTypeControlOptions: {mapTypeIds:[google.maps.MapTypeId.ROADMAP, google.maps.MapTypeId.SATELLITE]},
			mapTypeId: mapTypeId,
			mapTypeControl: details.showMapType,
			zoomControl: details.showZoom,
			scaleControl: details.showScale,
		   	streetViewControl: details.showStreetView
		});
		
		// add it to the collections
		_maps[id] = map;
		
		// turn off the labels for all points of interest
		map.setOptions({'styles':[{featureType:"poi",elementType: "labels",stylers:[{visibility:"off"}]}]});
		
		// get any map click event listener
		var f_click = window["Event_mapClick_" + id];
		// if there is a map click event listener
		if (f_click) {
			// attach a listener to the mapClick event
			google.maps.event.addListener(map, 'click', function() {
				// fire mapClick event
	    		f_click($.Event("mapClick"));
			});
		}
		
		// get any map drag start event listener
		var f_dragStart = window["Event_dragStart_" + id];
		// if there is a map drag event listener
		if (f_dragStart) {
			// attach a listener to the dragstart event
			google.maps.event.addListener(map, 'dragstart', function() {
				// fire touch event
	    		f_dragStart($.Event("drag"));
			});
		}
		
		// get any map drag end event listener
		var f_dragEnd = window["Event_dragEnd_" + id];
		// if there is a map drag event listener
		if (f_dragEnd) {
			// attach a listener to the dragstart event
			google.maps.event.addListener(map, 'dragend', function() {
				// fire touch event
	    		f_dragEnd($.Event("drag"));
			});
		}
		
		// a listener to stop the zoom on bounds being too small
		var zoomChangeBoundsListener = google.maps.event.addListenerOnce(map, 'bounds_changed', function(event) {		    	
			// wait a second for map to draw as it's async
			setTimeout(function(){ 
				// if zoom is greater than 18 (higher values are closer and more is too close)
				if (map.getZoom() > 18 ){
					map.setZoom(18);  // set zoom to max
				}
			},1000);		        
		});
		
	} else {

		$("#" + id).html("Map not available");

	}	
	
}

// get the standard map position object a data object a varierty of ways, including the async geocoder
function getMapPosition(data, rowIndex, callBack, map, details, zoomMarkers) {
	// create a basic position object
	var pos ={s:null};
	// check the data object
	if (data && data.fields && data.rows && data.rows.length > rowIndex) {
		// loop the fields
		for (var i in data.fields) {
			// get the field
			var f = data.fields[i];
			// if there is a field
			if (f) {
				// make it lower case
				f = f.toLowerCase();
				// check for latitude
				if (f == "lat" || f == "latitude") pos.lat = data.rows[rowIndex][i];
				// check for longditude
				if (f == "lng" || f == "lon" || f == "longitude") pos.lng = data.rows[rowIndex][i];
				// do the easting checks
				if (f == "e" || f == "east" || f == "easting" || f == "eastings") pos.e = data.rows[rowIndex][i];
				// do the northing checks
				if (f == "n" || f == "north" || f == "northing" || f == "northings") pos.n = data.rows[rowIndex][i];
				// do the title checks
				if (f == "title") pos.title = data.rows[rowIndex][i];
				// do the info checks
				if (f == "info") pos.info = data.rows[rowIndex][i];
				// do the icon checks
				if (f == "icon") pos.icon = data.rows[rowIndex][i];
			}
		}
		// if not lat and lng 
		if (!pos.lat && !pos.lng) {
			// if there is e and n
			if (pos.e && pos.n) {
				// since we have eastings and northings, convert				
				var pIn = new GT_OSGB();
				pIn.setGridCoordinates(pos.e, pos.n);
				var pOut = pIn.getWGS84();
				pos.lat = pOut.latitude;
				pos.lng = pOut.longitude;												
			} else {								
				// if there is not currently a search term
				if (!pos.s) {
					// if there are search fields
					if (details && details.searchFields) {
						// get the fields
						var fields = details.searchFields.split(",");
						// loop them
						for (var i in fields) {
							// get this search field
							var searchField = fields[i].trim();
							// if there is one
							if (searchField) {
								// loop the data fields
								for (var j in data.fields) {
									// get this field
									var field = data.fields[j];
									// if there is one and it matches
									if (field && field.toLowerCase() == searchField.toLowerCase()) {
										// set the value in this field to the search term
										pos.s = data.rows[rowIndex][j];
										// we're done
										break;
									}
								}
								// if we have a position search term, we're done
								if (pos.s) break;
							}
						}
					} else if (data.fields.length == 1) {
						// if this is a simple data object
						pos.s = data.rows[0][0];
					} 
				} // no search term
				// if there is a callback (getting positions for navigate to won't have one so will avoid the geo-coder) and a search term
				if (callBack && pos.s) {
					// create the geocoder if we don't have one already
					if (!_geocoder) _geocoder = new google.maps.Geocoder();
					// geocode the search term
					_geocoder.geocode( new mapGeocodeRequest(pos.s), function(results, status) {
					    if (status == google.maps.GeocoderStatus.OK) {							
							for (var i in mapGeocodeResults(results)) {
								var result = results[i];
								if (result.geometry && result.geometry.location) {
									var l = result.geometry.location;
									pos.lat = l.lat();
									pos.lng = l.lng();								
									callBack(map, pos, details, data, rowIndex, zoomMarkers);								
								}
							}      	
						} 
					 });
				}
			}
		}		
	}
	return pos;	
}

// this function can be overridden if a more sophisticated GeocoderRequest with bounds or restrictions is required
function mapGeocodeRequest(address) {
	this.address = address;
}

// this function can be overidden if a sophisticated filter of results is required
function mapGeocodeResults(results) {
	return results;
}

// set the map centre, used by both the setData method and the getPosition callback
function setMapCentre(map, pos) {
	if (map && pos && pos.lat && pos.lng) {		
		var latlng = new google.maps.LatLng(pos.lat, pos.lng);
		map.panTo( latlng );
	}
}

// add a map marker, used by both the addMapMarkers function (from the properties), and the getPosition callback
function addMapMarker(map, pos, details, data, rowIndex, zoomMarkers) {
	// skip if no lat and lng - but we might be coming back if we then searched
	if (map && pos && pos.lat && pos.lng) {

		// if this is heatmap
		if (details.heatmap) {
			
			// if there isn't a heatmap object in the map yet
			if (!map.heatmap) {
				
				// heatmap options
				var options = {
					data: [],
				    map: map
				};
				
				// check and add further options from details
				if (details.maxIntensity) options.maxIntensity = details.maxIntensity;
				if (details.radius) options.radius = details.radius;
				if (details.opacity) options.opacity = details.opacity;
				options.dissipating = details.dissipating;
			
				// create the heatmap
				var heatmap = new google.maps.visualization.HeatmapLayer(options);
	
				// retain a reference to it in the map
				map.heatmap = heatmap;
				
			}
			
			// make this point
			var point = new google.maps.LatLng(pos.lat, pos.lng);
			
			// add this point to heatmap
			map.heatmap.data.push(point);
			// add it to map markers too
			map.markers.push(point);
						
		} else {
		
			var markerOptions = {
				map: map,
				position: new google.maps.LatLng(pos.lat, pos.lng)
			};
			
			if (pos.title) markerOptions.title = pos.title;
			if (pos.icon) markerOptions.icon = pos.icon;
			if (details.markerImage) markerOptions.icon = "applications/" + _appId + "/" + _appVersion + "/" + details.markerImage;
			
			var marker = new google.maps.Marker(markerOptions);	
			marker.index = map.markers.length;
			marker.data = {fields:data.fields,rows:[data.rows[rowIndex]]};
			
			map.markers.push(marker);
			
			if (pos.info) {
				google.maps.event.addListener(marker, 'click', function() {
                    if (map.markerInfoWindow) map.markerInfoWindow.close();
                    var markerInfoWindow = new google.maps.InfoWindow({
                        content: pos.info
                    });
                    map.markerInfoWindow = markerInfoWindow;
                    markerInfoWindow.open(map,marker);
                });
			}
			
			if (details.markerClickFunction) {
				google.maps.event.addListener(marker, 'click', function() {
					map.markerSelectedIndex = marker.index;
				    window[details.markerClickFunction]($.Event('markerclick'));
				});
			}

		} // heatmap check
		
		// if we have all the markers now, zoom and centre them - this seems to only do up to 10, it might have something to do with search limits
		if (map.markers.length > 0 && map.markers.length == zoomMarkers) {
			if (zoomMarkers == 1) {
				// single marker, check getPosition is present
				if (map.markers[0].getPosition) {
					// get the latlng position
					var llPos = map.markers[0];
					// if getPosition is present use that
					if (llPos.getPosition) llPos = llPos.getPosition();
					// only if valid values
					if (llPos.lat && llPos.lng && llPos.lat() != 0 && llPos.lng() != 0) map.panTo(llPos);
				}
			} else {
				// multiple markers, use bounds
				var bounds = new google.maps.LatLngBounds();
				// loop the map markers
				for (var i in map.markers) {
					// get latlng position from marker
					var llPos = map.markers[i];
					// if getPosition is present use that
					if (llPos.getPosition) llPos = llPos.getPosition();
					// only if valid object and values
					if (llPos.lat && llPos.lng && llPos.lat() != 0 && llPos.lng() != 0) bounds.extend(llPos);
				}
				// fit to the bounds
				map.fitBounds(bounds);
			}
			
		} // finished markers check
		
	}
	
}

// adds markers to a map, used by add markers and replace markers
function addMapMarkers(map, data, details, zoomMarkers) {
	if (data && data.rows) {
		for (var i in data.rows) {
			var pos = getMapPosition(data, i, addMapMarker, map, details, zoomMarkers);
			addMapMarker(map, pos, details, data, i, zoomMarkers);
		}
	}
}            
