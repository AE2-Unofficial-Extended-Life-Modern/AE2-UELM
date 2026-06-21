package appeng.client.gui;

import static appeng.client.gui.MathExpressionParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MathExpressionParserTest {

    @ParameterizedTest
    @CsvSource(value = {
            "1 + 2|3",
            "3 *4 |12",
            "1 + 2 * 3 |7",
            "1 - 6|-5",
            "1/3|0.333333",
            "23.4 + 0.6|24",
            "1 - -4|5",
            "1 + 4*3*2|25",
            "1/0|failed",
            "1/(1 - 1)|failed",
            "3 + 2 * 4 - 1 /2|10.5",
            "1 + (2 * (2 * (1 + 1)))|9",
            "arkazkdhz|failed",
            "1 + 2 3 7 - 1|failed",
            "2 + + 2|failed",
            "10e6|failed",
            "-1 -1|-2",
            "- (1 + 1)|-2",
            "2 * -1|-2",
            "2 -2|0",
            "-  1|-1",
            "-1|-1",
            "- - - - - 5|-5",
            "-(-(-(-2)))|2",
            "1 - -1|2",
            "1 + -(2|failed",
            "NaN|failed",
            "1 / 0|failed",
            "64/4|16",
            "-2^2|-4",
            "2^2*3|12",
            "2^3.1|failed",
            "2^31|failed",
            "2^3^4|4096", // a bit unusual but acceptable
            "2^30^30^30|failed",
            "2^-1|failed",
    }, delimiter = '|')
    void testThird(String expression, String expected) {

        DecimalFormat format = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));
        format.setParseBigDecimal(true);
        format.setNegativePrefix("-");

        var parsed = parse(expression, format);
        if (parsed.isPresent()) {
            assertEquals(expected, format.format(parsed.get()));
        } else {
            assertEquals(expected, "failed");
        }

    }

    MathExpressionParser.Context ctxEN = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.US));
    MathExpressionParser.Context ctxFR = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.FRENCH));
    MathExpressionParser.Context ctxES = new MathExpressionParser.Context()
            .setNumberFormat(NumberFormat.getNumberInstance(Locale.forLanguageTag("ES")));

    @Test
    void NumbersBasic_Test() {
        assertEquals(41, parse("41"));
        assertEquals(42, parse("  42  "));

        assertEquals(1000000, parse("1 000 000"));
        assertEquals(1000000, parse("1_000_000"));

        assertEquals(123456.789, parse("123456.789", ctxEN));
        assertEquals(234567.891, parse("234,567.891", ctxEN));

        assertEquals(345678.912, parse("345 678,912", ctxFR));

        String s = NumberFormat.getNumberInstance(Locale.FRENCH).format(456789.123);
        assertEquals(456789.123, parse(s, ctxFR));

        assertEquals(567891.234, parse("567.891,234", ctxES));
    }

    @Test
    void ArithmeticBasic_Test() {
        assertEquals(5, parse("2+3"));
        assertEquals(-1, parse("2-3"));
        assertEquals(6, parse("2*3"));
        assertEquals(2, parse("6/3"));
        assertEquals(8, parse("2^3"));
    }

    @Test
    void UnaryMinus_Test() {
        assertEquals(-5, parse("-5"));
        assertEquals(-3, parse("-5+2"));
        assertEquals(-7, parse("-5-2"));
        assertEquals(-15, parse("-5*3"));
        assertEquals(-2.5, parse("-5/2"));
        assertEquals(-25, parse("-5^2")); // ! this is -(5^2), not (-5)^2.

        assertEquals(16, parse("(-4)^2"));
        assertEquals(-64, parse("(-4)^3"));

        assertEquals(2, parse("4+-2"));
        assertEquals(6, parse("4--2"));

        assertEquals(7, parse("--7"));
        assertEquals(-8, parse("---8"));
    }

    @Test
    void UnaryPlus_Test() {
        assertEquals(5, parse("+5"));
        assertEquals(7, parse("+5+2"));
        assertEquals(3, parse("+5-2"));
        assertEquals(15, parse("+5*3"));
        assertEquals(2.5, parse("+5/2"));
        assertEquals(25, parse("+5^2"));

        assertEquals(6, parse("4++2"));
        assertEquals(2, parse("4-+2"));

        assertEquals(7, parse("++7"));
        assertEquals(8, parse("+++8"));
    }

    @Test
    void ArithmeticPriority_Test() {
        assertEquals(4, parse("2+3-1"));
        assertEquals(14, parse("2+3*4"));
        assertEquals(10, parse("2*3+4"));
        assertEquals(7, parse("2^3-1"));
        assertEquals(13, parse("1+2^3+4"));

        // a^b^c = a^(b^c)
        assertEquals(262_144, parse("4^3^2"));
    }

    @Test
    void Brackets_Test() {
        assertEquals(5, parse("(2+3)"));
        assertEquals(20, parse("(2+3)*4"));
        assertEquals(14, parse("2+(3*4)"));
        assertEquals(42, parse("(((42)))"));

        assertEquals(14, parse("2(3+4)"));
    }

    @Test
    void ScientificBasic_Test() {
        assertEquals(2000, parse("2e3"));
        assertEquals(3000, parse("3E3"));
        assertEquals(0.04, parse("4e-2"));
        assertEquals(0.05, parse("5E-2"));
        assertEquals(6000, parse("6e+3"));

        assertEquals(6000, parse("6 e 3"));
        assertEquals(7800, parse("7.8e3"));
        assertEquals(90_000, parse("900e2"));
        assertEquals(1, parse("1e0"));
    }

    @Test
    void ScientificArithmetic_Test() {
        assertEquals(4000, parse("2*2e3"));
        assertEquals(6000, parse("2e3 * 3"));
        assertEquals(-200, parse("-2e2"));
        assertEquals(1024, parse("2^1e1"));

        // Not supported, but shouldn't fail. (2e2)e2 = 200e2 = 20_000.
        assertEquals(20_000, parse("2e2e2"));
    }

    @Test
    void SuffixesBasic_Test() {
        assertEquals(2000, parse("2k"));
        assertEquals(3000, parse("3K"));
        assertEquals(4_000_000, parse("4m"));
        assertEquals(5_000_000, parse("5M"));
        assertEquals(6_000_000_000D, parse("6b"));
        assertEquals(7_000_000_000D, parse("7B"));
        assertEquals(8_000_000_000D, parse("8g"));
        assertEquals(9_000_000_000D, parse("9G"));
        assertEquals(10_000_000_000_000D, parse("10t"));
        assertEquals(11_000_000_000_000D, parse("11T"));

        assertEquals(12 * 64, parse("12s"));
        assertEquals(13 * 64, parse("13S"));
        assertEquals(14 * 144, parse("14i"));
        assertEquals(15 * 144, parse("15I"));

        assertEquals(16 * 64 + 16, parse("16.25s", ctxEN));
        assertEquals(17 * 144 + 72, parse("17.5i", ctxEN));

        assertEquals(2050, parse("2.05k", ctxEN));
        assertEquals(50, parse("0.05k", ctxEN));
        assertEquals(3000, parse("3 k"));
    }

    @Test
    void SuffixesArithmetic_Test() {
        assertEquals(2005, parse("2k+5"));
        assertEquals(2005, parse("5+2k"));
        assertEquals(4000, parse("2k*2"));
        assertEquals(4000, parse("2*2k"));
        assertEquals(-2000, parse("-2k"));

        assertEquals(3_000_000, parse("3kk"));
        assertEquals(4_000_000_000D, parse("4kkk"));

        // Ideally we would want "1/9i" to parse into this too, but that would require a larger rework.
        // Currently, suffixes are hardcoded to have the highest priority.
        assertEquals(16, parse("(1/9)i"));

        // Not supported, but shouldn't fail.
        assertEquals(6_000_000_000d, parse("6km"));
        assertEquals(500_000, parse("0.5ke3", ctxEN));

        // Please don't do this.
        assertEquals(20_000_000_000D, parse("2e0.01k", ctxEN));
    }

    @Test
    void Percent_Test() {
        ctxEN.setHundredPercent(1000);

        assertEquals(100, parse("10%", ctxEN));
        assertEquals(2000, parse("200%", ctxEN));
        assertEquals(-300, parse("-30%", ctxEN));

        assertEquals(450, parse("40% + 50", ctxEN));
        assertEquals(500, parse("(20+30)%", ctxEN));
    }
}
