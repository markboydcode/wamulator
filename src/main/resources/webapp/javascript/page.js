var USER_VALIDATION_REGEX = "[#\\s\\,\\\"<>\\\\;\\+'&/]";
var USERNAME_MIN_LENGTH = 4;
var USERNAME_MAX_LENGTH = 97;
var OB_FORM_LOGIN_HASH_COOKIE_NAME = "ObFormLoginHashCookie";
var languageModalHeight;
var languageModalUlHeight;


$(document).ready(
		

		
function() {

    $("#username").focus();

    if (window.location.href.indexOf("error=") != -1) {
		$("#errorToolTip").fadeIn(1200);
		$('.inputText').addClass("error");
	}
	
	languageModalHeight = $('#languageModal').height();
	languageModalUlHeight = $('#languageModal ul').height();
	$('#languageModal').height(0);
	$('#languageModal ul').addClass('invisible');
	
	
	$('#cancelErrorImage').click(function() {
		$('#errorToolTip').fadeOut(300,function(){
			$('.inputText').removeClass("error");
		});
        $("#username").focus();
	});
	
	
	$('.modal').click(function() {
		var dirToFill;
		var otherDir;
		var destHeight;
		
		var href = $(this).attr("href");
		if (href == "conditions.html") {
			dirToFill = $('#rights');
			otherDir = $('#privacy');
		} else if (href == "privacy.html") {
			dirToFill = $('#privacy');
			otherDir = $('#rights');
		} else {
			return;
		}
		dirToFill.removeClass("invisible");
		otherDir.addClass("invisible");
		if ($(window).width() < 961) {
			destHeight = dirToFill.height() + 20;
			if (destHeight == 20) {
				destHeight = 500;
			}
			
			$('#legalViewer').css("overflow-y","visible");
		} else {
			destHeight = 340;
			$('#legalViewer').css("overflow-y","scroll");
		}
		
		
		
		
		$('#content1').animate(
			{height: (destHeight + 60) + "px", paddingTop: "0px"}, //paddingBottom: "0px"},
			500,
			function() {
				if ($(window).width() < 961 && dirToFill.html() != "") {
					$('#content1').height("100%");
				}
			}
		);
		
		//if the modal isn't open
		if ($('#legalViewer').hasClass('hidden')) {
			$('#legalViewer').toggleClass('hidden');
			$('#content1').toggleClass("behind");
			
			$('#login').animate({height: "0px", opacity: "0"}, 300);
			
			$('#legalViewer').animate(
				{opacity: "1", height: destHeight + "px", borderBottomWidth: "20px", borderTopWidth: "40px"},
				600,
				function() {
                    $('#closeLegalViewer').toggleClass('hidden');
					if ($(window).width() < 961)
						$('#legalViewer').height("100%");
				}
			);
		} else {
			$('#legalViewer').animate(
				{height: destHeight + "px", borderBottomWidth: "20px", borderTopWidth: "40px"},
				500,
				function() {
					if ($(window).width() < 961)
						$('#legalViewer').height("100%");
				}
			);
		}
		
		if (dirToFill.html() == "") {
			$.ajax({
				url:href,
				dataType: "html",
				success: function(html) {
					dirToFill.html(html);
					if ($(window).width() < 961) {
						height = dirToFill.height();
						$('#content1').animate(
							{height: (height+60) + "px", paddingTop: "0px"}, //paddingBottom: "0px"},
							400,
							function() {
								$('#content1').height("100%");
							}
						);
						$('#legalViewer').animate(
							{height: (height+20) + "px", borderBottomWidth: "20px", borderTopWidth: "40px"},
							400,
							function() {
								$('#legalViewer').height("100%");
							}
						);
					}
					
				}
			});
		}
		
		
		return false;
	});
	
	$('#closeLegalViewer').click(function() {
		
		if ($(window).width() < 961) {
		//	height= "116px";
			padB = "20px";
		} else {
		//	height = "58px";
			padB = "0px";
		}
		height = "100%";
		
		$('#content1').animate(
        		{height: height, paddingTop: "20px"}, //paddingBottom: padB},
        		500
        	);
		$('#content1').toggleClass("behind");
		
        $('#login').animate(
             {height: height},
             300
        );
        $('#login').animate(
        	{opacity: "1"},
        	500
        );
        	
        $('#legalViewer').animate(
        		{height: "0px", opacity: "0",  borderBottomWidth: "0px", borderTopWidth: "0px"},
        		300,
        		function(){
        			$('#legalViewer #rights').addClass("invisible");
        			$('#legalViewer #privacy').addClass("invisible");
        		}
        	);
        	$('#legalViewer').toggleClass('hidden');
            $('#closeLegalViewer').toggleClass('hidden');
	});
	
   $(document).mouseup(function (e)
    {
        var container1 = $("#languageModal");
        var container2 = $("#languageMenu");
        if (container1.has(e.target).length === 0 && container2.has(e.target).length === 0 && container1.hasClass("visible"))
        {
            container1.toggleClass("visible gone");
            container1.animate(
                {height: 0, opacity: 0},
                250
            );
            $('#languageModal ul').animate(
                {height: 0},
                250,
                function (){
                	$('#languageModal ul').addClass('invisible');
                }
            );
            $('#changeLanguageDrop').toggleClass("normal selected");
        }
        
    });
    
    $('#changeLanguageDrop').click(function() {
    	
    	$('#changeLanguageDrop').toggleClass("normal selected");
    	$("#languageModal").toggleClass("visible gone");
    	
    	var toHeight;
    	var toHeightUl;
    	var toOpacity;
    	if ($("#languageModal").hasClass("gone")) {
    		toHeight = toHeightUl = 0;
    		toOpacity = 0;
    	} else {
    		toHeight = languageModalHeight;
    		toHeightUl = languageModalUlHeight;
    		toOpacity = 1;
    		$('#languageModal ul').removeClass('invisible');
    	}

    	$('#languageModal').animate(
            {height: toHeight, opacity: toOpacity},
            250
        );
    	$('#languageModal ul').animate(
            {height: toHeightUl},
            250,
            function (){
            	if ($('#languageModal').hasClass('gone')) {
            		$('#languageModal ul').addClass('invisible');
            	}
            }
        );
    });
    
    $('#submit').click(function() {
        var valid = validCredentials();
        if (!valid) {
            $('#scriptValidationErroMsgText').html($('#emptyCredsMsg').html());
            $("#errorToolTip").fadeIn(1200);
            $('.inputText').addClass("error");
        } else {
            $(this).disable;
        }
        return valid;
    });

    $('.trigger').click(function() {
        $('.alert').hide();
        var id = $(this).attr('id');
        var alertID = id + '-alert';
        $('#' + alertID).fadeIn();
    });

    if (!("autofocus" in $('input'))) {
        $('input:text:visible:first').focus();
    }

    // Set ObFormLoginHashCookie if fragment exists
    var hash = window.location.hash;
    if (hash != "") {
        setCookie(OB_FORM_LOGIN_HASH_COOKIE_NAME, encodeURI(hash), 0);
    }
});

function validCredentials() {
    var username = $('#username').val();
    if (username.length < USERNAME_MIN_LENGTH || username.length > USERNAME_MAX_LENGTH) {
        return false;
    }

    var password = $("#password").val();
    if (password.length < 1) {
        return false;
    }
    // look for invalid characters and return false if any are seen
    return username.search(USER_VALIDATION_REGEX) < 0;
}

function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        var expires = "; expires=" + date.toGMTString();
    }
    document.cookie = name + "=" + value + expires + "; path=/";
}

function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) == 0) {
            return c.substring(nameEQ.length, c.length);
        }
    }
    return null;
}