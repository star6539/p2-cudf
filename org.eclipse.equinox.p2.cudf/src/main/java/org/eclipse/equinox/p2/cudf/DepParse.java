package org.eclipse.equinox.p2.cudf;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.equinox.p2.cudf.metadata.*;

public class DepParse {
	public static void main(String[] args) {
		ArrayList ands = new ArrayList();
		//c | x != 2
		StringTokenizer s = new StringTokenizer("x, a > 2, b = 3, f != 4, c | d, e < 5", ",");
		while (s.hasMoreElements()) {
			StringTokenizer subTokenizer = new StringTokenizer(s.nextToken(), "|");
			if (subTokenizer.countTokens() == 1) {
				ands.add(createRequire(subTokenizer.nextToken()));
				continue;
			}

			IRequiredCapability[] ors = new RequiredCapability[subTokenizer.countTokens()];
			int i = 0;
			while (subTokenizer.hasMoreElements()) {
				ors[i++] = (IRequiredCapability) createRequire(subTokenizer.nextToken());
			}
			ands.add(new ORRequirement(ors));
		}
		System.out.println(ands);
	}

	private static Object createRequire(String nextToken) {
		//>, >=, =, <, <=, !=
		StringTokenizer expressionTokens = new StringTokenizer(nextToken.trim(), ">=!<", true);
		int tokenCount = expressionTokens.countTokens();
		if (tokenCount == 1)
			return new RequiredCapability(expressionTokens.nextToken(), VersionRange.emptyRange);
		if (tokenCount == 3)
			return new RequiredCapability(expressionTokens.nextToken(), createRange3(expressionTokens.nextToken(), expressionTokens.nextToken()));
		if (tokenCount == 4) {
			String id = expressionTokens.nextToken();
			String signFirstChar = expressionTokens.nextToken();
			expressionTokens.nextToken();//skip second char of the sign
			VersionRange range = createRange4(signFirstChar, expressionTokens.nextToken());
			if (range != null)
				return new RequiredCapability(id, range);
		}
		return null;
	}

	private static VersionRange createRange3(String sign, String versionAsString) {
		int version = Integer.decode(versionAsString.trim()).intValue();
		sign = sign.trim();
		if (">".equals(sign))
			return new VersionRange(new Version(version), false, Version.maxVersion, false);
		if ("<".equals(sign))
			return new VersionRange(Version.emptyVersion, false, new Version(version), false);
		if ("=".equals(sign))
			return new VersionRange(new Version(version));
		throw new IllegalArgumentException(sign);
	}

	private static VersionRange createRange4(String sign, String versionAsString) {
		int version = Integer.decode(versionAsString).intValue();
		if (">".equals(sign)) //THIS IS FOR >=
			return new VersionRange(new Version(version), true, Version.maxVersion, false);
		if ("<".equals(sign)) //THIS IS FOR <=
			return new VersionRange(Version.emptyVersion, false, new Version(version), true);
		return null;
	}
}
