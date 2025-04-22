const DATE_REGEX = /^\d{6}$/;

export function convertMRZData(input: {
	documentNumber: string;
	birthDate: string;
	expiryDate: string;
}) {
	try {
		const formatDate = (dateString: string) => {
			const parts = dateString.split(' ')[0]?.split('-');
			const year = parts?.[0]?.slice(-2);
			const month = parts?.[1];
			const day = parts?.[2];
			return `${year}${month}${day}`;
		};

		return {
			documentNumber: input.documentNumber,
			dateOfBirth: formatDate(input.birthDate),
			dateOfExpiry: formatDate(input.expiryDate),
		};
	} catch (error) {
		console.error('Error in convertMRZData:', error);
		throw new Error('Failed to convert MRZ data.');
	}
}

export function isDate(str: string) {
	try {
		return typeof str === 'string' && DATE_REGEX.test(str);
	} catch (error) {
		console.error('Error validating date:', error);
		return false;
	}
}

export function assert(statement: boolean, err: string) {
	if (!statement) {
		throw new Error(err || 'Assertion failed');
	}
}

export function extractMRZInfo(mrzString: string) {
	try {
		const mrzLines = mrzString.split('\n');

		if (mrzLines.length < 2) {
			throw new Error('Invalid MRZ format: Expected two lines of MRZ data');
		}

		const documentNumber = mrzLines[1]?.slice(0, 9).replace(/</g, '').trim();
		const birthDate = mrzLines[1]?.slice(13, 19).trim();
		const expiryDate = mrzLines[1]?.slice(21, 27).trim();

		return {
			documentNumber: documentNumber as string,
			dateOfBirth: birthDate as string,
			dateOfExpiry: expiryDate as string,
		};
	} catch (error) {
		console.error('Error extracting MRZ info:', error);
		throw new Error('Failed to extract MRZ information.');
	}
}
