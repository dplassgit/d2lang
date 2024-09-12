package com.plasstech.lang.d2.optimize;

/**
 * When comparing single characters of a string, optimizations can be done:
 * 
 * <pre>
 * temp1 = s[i]
 * temp2 = temp1 == 'h'
 * </pre>
 * 
 * becomes
 * 
 * <pre>
 * temp1 = s[i]
 * temp3 = asc(temp1)
 * temp4 = asc('h')
 * temp2 = temp1 == temp4
 * </pre>
 * 
 * And if i is 0,
 * 
 * <pre>
 * temp1 = asc(s)
 * temp3 = asc('h')
 * temp2 = temp1 == temp3
 * </pre>
 * 
 */
public class StringCompareOptimizer extends LineOptimizer {
  StringCompareOptimizer(int debugLevel) {
    super(debugLevel);
  }
}
