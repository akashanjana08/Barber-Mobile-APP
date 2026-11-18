const TIMEZONE = 'Asia/Kolkata';

const MONTH_MAP = {
  jan: 1, january: 1,
  feb: 2, february: 2,
  mar: 3, march: 3,
  apr: 4, april: 4,
  may: 5,
  jun: 6, june: 6,
  jul: 7, july: 7,
  aug: 8, august: 8,
  sep: 9, sept: 9, september: 9,
  oct: 10, october: 10,
  nov: 11, november: 11,
  dec: 12, december: 12,
};

/**
 * Returns today's calendar date parts (year, month, day) in Asia/Kolkata.
 */
function getKolkataDateParts(date = new Date()) {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  const parts = formatter.format(date).split('-').map(Number);
  return {
    year: parts[0],
    month: parts[1], // 1-12
    day: parts[2],   // 1-31
  };
}

/**
 * Normalizes calendar parts to a comparable UTC timestamp (midnight).
 */
function toMidnightTimestamp(year, month, day) {
  return Date.UTC(year, month - 1, day);
}

/**
 * Parses various date string representations into { year, month, day }.
 */
function parseDateString(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return null;

  const currentKolkata = getKolkataDateParts();
  let clean = dateStr.trim();

  // Strip extraneous labels like "(Today)", "Today,", etc.
  clean = clean.replace(/\(Today\)|\(Tomorrow\)/gi, '').trim();
  if (clean.toLowerCase().startsWith('today,')) {
    clean = clean.substring(6).trim();
  } else if (clean.toLowerCase() === 'today') {
    return currentKolkata;
  } else if (clean.toLowerCase() === 'tomorrow') {
    const tomorrowDate = new Date(Date.now() + 86400000);
    return getKolkataDateParts(tomorrowDate);
  }

  // 1. ISO format: YYYY-MM-DD
  const isoMatch = clean.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (isoMatch) {
    return {
      year: parseInt(isoMatch[1], 10),
      month: parseInt(isoMatch[2], 10),
      day: parseInt(isoMatch[3], 10),
    };
  }

  // 2. DD-MM-YYYY or DD/MM/YYYY
  const dmyMatch = clean.match(/^(\d{1,2})[-/](\d{1,2})[-/](\d{4})/);
  if (dmyMatch) {
    return {
      day: parseInt(dmyMatch[1], 10),
      month: parseInt(dmyMatch[2], 10),
      year: parseInt(dmyMatch[3], 10),
    };
  }

  // 3. Named month formats: "29 September 2026", "29 Sep 2026", "01 October 2026", "29 Sep"
  const textMatch = clean.match(/^(\d{1,2})\s+([a-zA-Z]+)(?:\s+(\d{4}))?/);
  if (textMatch) {
    const day = parseInt(textMatch[1], 10);
    const monthKey = textMatch[2].toLowerCase();
    const month = MONTH_MAP[monthKey];
    const year = textMatch[3] ? parseInt(textMatch[3], 10) : currentKolkata.year;

    if (month) {
      return { year, month, day };
    }
  }

  return null;
}

/**
 * Validates if the given date is within today and the next 2 days in Asia/Kolkata.
 * Range: [today, today + 2 days] (inclusive, total 3 days).
 */
function isDateWithinAllowedWindow(dateStr) {
  const parsed = parseDateString(dateStr);
  if (!parsed) return false;

  const current = getKolkataDateParts();
  const todayTimestamp = toMidnightTimestamp(current.year, current.month, current.day);
  const maxTimestamp = todayTimestamp + 2 * 86400000; // today + 2 days
  const targetTimestamp = toMidnightTimestamp(parsed.year, parsed.month, parsed.day);

  return targetTimestamp >= todayTimestamp && targetTimestamp <= maxTimestamp;
}

/**
 * Returns formatted 3 upcoming dates in Asia/Kolkata.
 */
function getAvailableBookingDates() {
  const current = getKolkataDateParts();
  const dates = [];

  for (let i = 0; i <= 2; i++) {
    const d = new Date(Date.UTC(current.year, current.month - 1, current.day + i));
    const day = String(d.getUTCDate()).padStart(2, '0');
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const monthName = monthNames[d.getUTCMonth()];
    const year = d.getUTCFullYear();
    dates.push(`${day} ${monthName} ${year}`);
  }

  return dates;
}

module.exports = {
  TIMEZONE,
  getKolkataDateParts,
  parseDateString,
  isDateWithinAllowedWindow,
  getAvailableBookingDates,
};
