const PROPERTY_MINE = 5;

function initModifiedDate() {
    $(".modifiedDate").each(function() {
        $(this).text(moment($(this).text()).locale(portal.locale).format("LLLL"));
    });
    $(".modifiedDate").removeClass("hidden");
}
$(document).ready(function() {
    $("body").on("input", ".licenseForm input", function() {
        $(this).closest(".form-group").removeClass("has-error");
    });
    $("body").on("change", "#licenseCheckbox", function () {
        $("#licenseDatepicker").prop("disabled", $(this).is(":checked"));
    });

    initModifiedDate();
    $("body").on("change", ".radioProperty", function() {
        var selectedIndex = $(this).parent().parent().parent().index();
    });
});

function initDatepickerManageModal(endLicenseYear, endLicenseMonth, endLicenseDay) {
    let $targetInput = $("#licenseDatepicker");
    var opts = {
        input: $targetInput,
        useTime: 0,
        parseFormat: "YYYY-MM-DD",
        val: endLicenseYear + "-" + endLicenseMonth + "-" + endLicenseDay,
        allowEmptyDate: true,
        ashidden: {
            iso8601: "date_ISO8601"
        }
    };
    localDatePicker(opts);
    $targetInput.css("min-width",150);
}

function markInputError(elementId) {
    $("#" + elementId).closest(".form-group").addClass("has-error");
}

function clearManageModalForm() {
    $(".licenseForm").find("input, textarea").val("");
}

