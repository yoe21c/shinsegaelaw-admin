const myScript = (function () {

    const formId = '#form';
    console.log("my-script loaded ! formId : ", formId);

    //Initialize Select2 Elements
    $('.select2').select2();
    $('.select2bs4').select2({
        theme: 'bootstrap4'
    });

    $('[data-mask]').inputmask();

    let Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 1000
    });

    let Toast5Sec = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 5000
    });

    // 폼 이벤트 실행안되도록
    $(formId).submit(function( event ) {
        console.log('prevention default form submit event !')
        event.preventDefault();
        return;
    });

    let formSerialized = function(id) {
        let form = $(id);
        const disabled = form.find(':disabled').removeAttr('disabled');
        const serialized = form.serialize();
        disabled.attr('disabled', 'disabled');
        return serialized;
    };

    let editorUpdate = function(url, callback) {
        $.ajax({
            url: url,
            type:"GET",
            dataType : 'json',
            async: false,
            success:function(data){
                //console.log("editorUpdate result : ", data);
                myScript.Toast.fire({
                    icon: 'success',
                    title: '가져오기 성공!'
                }).then(function () {
                    callback(data);
                });
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '가져오기 실패! [' + errorMessage + ']'
                });
            },
        });
    }

    let create = function(formId, url, redirectUrl) {
        let serialized = formSerialized(formId);
        console.log("created ! formId: ", formId, "serialized : ", serialized);
        $.ajax({
            url: url,
            type:"POST",
            data: serialized,
            dataType : 'json',
            async: false,
            success:function(data){
                console.log("update result : ", data);
                myScript.Toast.fire({
                    icon: 'success',
                    title: '등록 성공!'
                }).then(function () {
                    location.href = redirectUrl;
                });
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '등록 실패! [' + errorMessage + ']'
                });
            },
        });
    }

    let update = function(formId, url, message) {
        let serialized = formSerialized(formId);
        $.ajax({
            url: url,
            type:"POST",
            data: serialized,
            dataType : 'json',
            async: false,
            success:function(data){
                console.log("update result : ", data);
                myScript.Toast.fire({
                    icon: 'success',
                    title: '업데이트 성공!'
                }).then(function () {
                    location.reload();
                })
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '업데이트 실패! [' + errorMessage + ']'
                });
            },
        });
    };

    let deletion = function(url, redirectUrl, callback) {
        $.ajax({
            url: url,
            type:"POST",
            // data:params,
            dataType : 'json',
            async: false,
            success:function(data){
                console.log("delete result : ", data);
                myScript.Toast.fire({
                    icon: 'success',
                    title: '삭제 성공!'
                }).then(function () {
                    if(redirectUrl !== '') {
                        location.href = redirectUrl;
                    }else if (callback !== undefined) {
                        callback();
                    }
                })
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '삭제 실패! [' + errorMessage + ']'
                });
            },
        });
    }


    let clone = function(url, redirectUrl, callback) {
        $.ajax({
            url: url,
            type:"POST",
            // data:params,
            dataType : 'json',
            async: false,
            success:function(result){
                console.log("clone result : ", result);
                const newSeq = result.data.newSeq;
                myScript.Toast.fire({
                    icon: 'success',
                    title: '복사 성공!'
                }).then(function () {
                    if(redirectUrl !== '') {
                        location.href = redirectUrl + "?seq=" + newSeq;
                    }else if (callback !== undefined) {
                        callback();
                    }
                })
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '복사 실패! [' + errorMessage + ']'
                });
            },
        });
    }


    let custom = function(url, data, callback) {
        $.ajax({
            url: url,
            type:"POST",
            data: data,
            dataType : 'json',
            async: false,
            success:function(result){
                callback(result.data);
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                if(response.responseJSON.message != undefined) {
                    errorMessage = response.responseJSON.message;
                }
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '조회 실패! [' + errorMessage + ']'
                });
            },
        });
    }

    /**
     * thymeleaf 를 기반으로 예를들어 /particles/departments.html 가 교체로 보여지도록 한다.
     * @param url
     * @param data
     * @param callback
     */
    let replace = function(url, replaceClass, callback) {
        $.ajax({
            url: url,
            type:"GET",
            dataType : 'html',
            async: false,
            success:function(result){
                $('.' + replaceClass).html(result);
                if(callback !== null) {
                    callback();
                }
            },
            error:function(response, status, error){
                let errorMessage = response.status;
                myScript.Toast5Sec.fire({
                    icon: 'error',
                    title: '조회 실패! [' + errorMessage + ']'
                });
            },
        });
    }


    let test = function () {
        alert('aaa');
    }

    return {
        Toast: Toast,
        Toast5Sec: Toast5Sec,
        editorUpdate: editorUpdate,
        create: create,
        update: update,
        deletion: deletion,
        clone: clone,
        test: test,
        custom: custom,
        replace: replace,
    }
}());

$.fn.serializeObject = function() {
    var result = {}
    var extend = function(i, element) {
        var node = result[element.name]
        if ("undefined" !== typeof node && node !== null) {
            if ($.isArray(node)) {
                node.push(element.value)
            } else {
                result[element.name] = [node, element.value]
            }
        } else {
            result[element.name] = element.value
        }
    }

    $.each(this.serializeArray(), extend)
    return result;
}

