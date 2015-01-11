$(".expand").click(function () {
    $(this).toggle();
    $(this).next().toggle();
    $(this).parent().parent().children().last().toggle();
});
$(".collapse").click(function () {
    $(this).toggle();
    $(this).next().toggle();
    $(this).parent().parent().children().last().toggle();
});
