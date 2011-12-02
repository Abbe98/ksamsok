package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_CENTURY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_DECADE;

import java.util.Date;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TimeUtil {

	private static final Logger logger = Logger.getLogger(TimeUtil.class);

	// iso8601-datumparser, tr�ds�ker
	private static final DateTimeFormatter isoDateTimeFormatter = ISODateTimeFormat.dateOptionalTimeParser();

	// konstanter f�r century och decade
	// fr�n vilken tidpunkt ska vi skapa s�rskilda index f�r �rhundraden och �rtionden
	static final Integer century_start = -1999;
	// till vilken tidpunkt ska vi skapa s�rskilda index f�r �rhundraden och �rtionden
	static final Integer century_stop = 2010;
	// str�ng med �rtal som m�ste vara f�re century_start
	static final String old_times = "-9999";

	static String parseYearFromISO8601DateAndTransform(String value) {
		String result = null;
		try {
			// tills vidare godk�nner vi ocks� "x f.kr" och "y e.kr" skiftl�gesok�nsligt h�r
			int dotkrpos = value.toLowerCase().indexOf(".kr");
			int year;
			if (dotkrpos > 0) {
				year = Integer.parseInt(value.substring(0, dotkrpos - 1).trim());
				switch (value.charAt(dotkrpos - 1)) {
				case 'F':
				case 'f':
					year = -year + 1; // 1 fkr �r �r 0
					// obs - inget break
				case 'E':
				case 'e':
					// TODO: "numerfixen" g�rs numera p� solr-sidan, �ndra returtyp?
					result = String.valueOf(year);
					//result = transformNumberToLuceneString(year);
					break;
					default:
						// m�ste vara f eller e f�r att s�tta result
				}
			} else {
				DateTime dateTime = isoDateTimeFormatter.parseDateTime(value);
				year = dateTime.getYear();
				// TODO: "numerfixen" g�rs numera p� solr-sidan, �ndra returtyp?
				result = String.valueOf(year);
				//result = transformNumberToLuceneString(year);
			}
			
		} catch (Exception ignore) {
			// l�ggs till som "problem" i addToDoc om denna metod returnerar null
		}
		return result;
	}

	// parse av iso8601-datum
	static Date parseISO8601Date(String dateStr) {
		Date date = null;
		if (dateStr != null) {
			try {
				DateTime dateTime = isoDateTimeFormatter.parseDateTime(dateStr);
				date = dateTime.toDate();
			} catch (Exception ignore) {
			}
		}
		return date;
	}

	// tolkar och indexerar ett iso-datum som yyyy-mm-dd om det inte ligger i framtiden
	static Date parseAndIndexISO8601DateAsDate(String identifier, String index, String dateStr, IndexProcessor ip) {
		Date date = parseISO8601Date(dateStr);
		if (date != null) {
			if (date.after(new Date())) {
				ContentHelper.addProblemMessage("The date in '" + index + "' for " + identifier  + " is in the future: " + dateStr);
			} else {
				ip.setCurrent(index);
				ip.addToDoc(ContentHelper.formatDate(date, false));
			}
		} else {
			// TODO: vill man ha med identifier i varningmeddelandet? Kan dock bli
			//       m�nga loggrader d� detta verkar vara ett vanligt fel
			ContentHelper.addProblemMessage("Could not interpret '" + index + "' as ISO8601: " + dateStr);
		}
		return date;
	}

	static void expandDecadeAndCentury(String fromTime, String toTime,
			String contextType, IndexProcessor ip) throws Exception {
		// timeInfoExists, decade och century
		if (fromTime != null || toTime != null) {
			// bara d� vi ska skapa �rhundraden och �rtionden
			Integer start=century_start, stop=century_stop;
			// start=senaste av -2000 och fromTime, om fromTime==null s� anv�nds -2000
			// stop= tidigaste av 2010 och toTime, om toTime==null s� anv�nds 2010
			String myFromTime=null, myToTime=null;
			if (fromTime!=null) myFromTime = new String(fromTime);
			if (toTime!=null) myToTime = new String(toTime);

			// om bara ena v�rdet finns s� �r det en tidpunkt, inte ett tidsintervall
			if (myFromTime==null) {
				myFromTime=myToTime;
			}
			if (myToTime==null) {
				myToTime=myFromTime;
			}
			
			myFromTime=tidyTimeString(myFromTime);
			start=latest(myFromTime, start);

			myToTime=tidyTimeString(myToTime);
			stop=earliest(myToTime, stop);
				
			Integer runner=start;
			Integer insideRunner=0;
			String dTimeValue=decadeString(runner);
			String cTimeValue=centuryString(runner);

			while (runner<=stop) {
				dTimeValue=decadeString(runner);
				ip.setCurrent(IX_DECADE, contextType);
				ip.addToDoc(dTimeValue);
				if (insideRunner%100==0){
					cTimeValue=centuryString(runner);
					ip.setCurrent(IX_CENTURY, contextType);
					ip.addToDoc(cTimeValue);
				}
				runner+=10;
				insideRunner+=10;
			}
			//slutvillkorskontroller
			if (!dTimeValue.equals(decadeString(stop)) && stop>century_start) {
				dTimeValue=decadeString(stop);
				ip.setCurrent(IX_DECADE, contextType);
				ip.addToDoc(dTimeValue);
			}
			if (!cTimeValue.equals(centuryString(stop)) && stop>century_start) {
				cTimeValue=centuryString(stop);
				ip.setCurrent(IX_CENTURY, contextType);
				ip.addToDoc(cTimeValue);
			}
		}
	}

	static Integer latest(String aString, Integer aInteger) throws Exception {
		Integer sLatest=0;
		try {
			sLatest=Math.max(Integer.parseInt(aString),aInteger);
		} catch (Exception e) {
			logger.error("Fel i fromTime: " + aString + " l�ngd: " + aString.length() + " : " + e.getMessage());
			throw e;
		}
		return sLatest;
	}

	static Integer earliest(String aString, Integer aInteger) throws Exception {
		Integer sEarliest=0;
		try {
			sEarliest=Math.min(Integer.parseInt(aString),aInteger);
		} catch (Exception e) {
			logger.error("Fel i toTime: " + aString + " l�ngd: " + aString.length() + " : " + e.getMessage());
			throw e;
		}
		return sEarliest;
	}

	static String decadeString(Integer aInteger) {
		Integer decadeFloor=(aInteger/10)*10;
		if (decadeFloor<0) decadeFloor-=10;
		String aDecade=String.valueOf(decadeFloor);
		return aDecade;
	}

	static String centuryString(Integer aInteger) {
		Integer centuryFloor=(aInteger/100)*100;
		if (centuryFloor<0) centuryFloor-=100;
		String aCentury=String.valueOf(centuryFloor);
		return aCentury;
	}
	
	static String tidyTimeString(String aString) throws Exception {
		String timeString=aString;
		try {
			if ((timeString.length()>5) && timeString.startsWith("-")) {
				// troligen �rtal f�re -10000
				timeString=old_times;
			}
			//else if (timeString.indexOf("-"==5)) {
			//	timeString=timeString.substring(0, 4);
			// (innefattas av n�sta case)
			//}
			else if (timeString.length()>4 && !timeString.startsWith("-")) {
				timeString=timeString.substring(0, 4);
			}
		}
		catch (Exception e) {
			logger.error("Fel i tidyTimeString: " + timeString + " : " + e.getMessage());
			throw e;
		}
		return timeString;
	}

}
