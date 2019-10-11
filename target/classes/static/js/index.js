$('#api-select').on('change', function() {
    $('.request-fields').hide();

    if (this.value === 'portfolio-post') {
        document.getElementById("form").reset();
        $("#id-div").show();
        $("#deviation-div").show();
        $("#type-div").show();
    }

    if (this.value === 'portfolio-get') {
        document.getElementById("form").reset();
        $("#id-div").show();
    }
});

$('select[name=typeSelect]').on('change', function() {
    if (this.value === 'fund') {
        $("#allocation-div").show();
    }
});

$('button[name=addAllocationBtn]').click(function() {
    var allocation = $("select[name=allocationSelect] option:selected").val();
    var percentage = $("input[name=percentageInput]").val();
    var str = "<li>" + allocation + " (" + percentage + "%)" + "<span class='close'>x</span></li>";
    $('#allocation-list ul').append(str);

    var closeBtn = $('span[class=close]');
    closeBtn.unbind('click');
    closeBtn.click(function() {
        this.parentElement.remove();
    });
});

$('button[name=submitBtn]').click(function() {
    var type;
    var test = $("select[name=apiSelect] option:selected");
    var api = $("select[name=apiSelect] option:selected").val();
    var allocations = {};

    $("#allocation-list ul li").each(function(idx, li) {
        var fundInfo = $(li)[0].innerText;
        var fundId = fundInfo.substring(0, fundInfo.indexOf(' '));
        var fundAmount = fundInfo.substring(fundInfo.indexOf('(')+1, fundInfo.indexOf('%'));
        allocations[fundId] = fundAmount;
    });

    if (api === 'portfolio-get') {
        type = "get";
    } else if (api === 'portfolio-post') {
        type = "post";
    }

    var data = {
        id: $("input[name=idInput]").val(),
        deviation: $("input[name=deviationInput]").val(),
        type: $("select[name=typeSelect] option:selected").text(),
        allocations: allocations
    };

    console.log(data);

    $.ajax({
        url: "/portfolio/" + $("input[name=idInput]").val(),
        data: type == 'post' ? JSON.stringify(data) : null,
        dataType: 'json',
        type: type,
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        success: function(result) {
            console.log("success");
            console.log(result);

            if (type == 'get') {

                var json = JSON.stringify(result);

                $('<p>' + json + '</p>').appendTo('#textbox-div');
                //$('#textbox-div').append(result);
            }
        }, error: function(status, error) {
            console.log(status);
            console.log(error);
        }
    });
});
