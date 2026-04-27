package dev.nathanlively.event_sourced_eclipsestore.domain.showbook;

import org.assertj.core.api.AbstractAssert;

public class ShowBookAssert extends AbstractAssert<ShowBookAssert, ShowBook> {
    protected ShowBookAssert(ShowBook actual) {
        super(actual, ShowBookAssert.class);
    }

    public static ShowBookAssert assertThat(ShowBook actual) {
        return new ShowBookAssert(actual);
    }

    public ShowBookAssert hasId(ShowBookId expectedId) {
        isNotNull();
        if (!actual.getId().equals(expectedId)) {
            failWithMessage("Expected document to have ID <%s> but was <%s>", expectedId, actual.getId());
        }
        return this;
    }
}
