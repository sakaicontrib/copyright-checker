$(document).ready(function() {
    var $colHeader = $("thead [data-header-id]");
    var uuid = $colHeader.data("header-id");
    var $colChecks = $("[data-col-uuid=" + uuid + "]");

    $colChecks.prop("checked", false);

    $colHeader.on("change", function () {
        var check = $colHeader.prop("checked");
        $colChecks.prop("checked", check);
    });

    $colChecks.on("change", function() {
        var disabled = ($("tbody [data-col-uuid=" + uuid + "]:checked").length > 0 ? false : true);
        $("#bulkActionsSubmit").prop("disabled", disabled);
    });

    $("#bulkActionsSubmit").click(function(e) {
        if (!confirm($("#bulkActionsConfirmMsg").text())) {
            e.preventDefault();
            return;
        }
    });
});
