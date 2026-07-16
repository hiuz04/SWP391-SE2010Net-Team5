// Lưu đường context của trang
const ctx = window.APP_CTX || "";

// load Feedback để có thông tin về feedback
function loadFeedback() {
    const params = new URLSearchParams(window.location.search);
    const feedbackId = params.get("id");

    if (!feedbackId) {
        return;
    }

    fetch(`${ctx}/feedback-user?action=get&id=${feedbackId}`)
        .then(res => res.json())
        .then(data => {

            // Lưu feedbackId
            document.getElementById("feedbackId").value = data.feedbackId;

            // Chọn số sao
            const rating = document.querySelector(
                `input[name="rating"][value="${data.rating}"]`
            );

            if (rating) {
                rating.checked = true;
            }

            // Nội dung đánh giá
            document.querySelector("textarea[name='description']").value =
                data.description ?? "";
        })
        .catch(err => {
            console.error(err);
            alert("Không thể tải thông tin đánh giá.");
        });
}

// Submit Form
function submitForm() {
    const feedbackId = document.getElementById("feedbackId").value;

    if (feedbackId && feedbackId.trim() !== "") {
        submitEditFeedback();
    } else {
        submitFeedback();
    }
}

// Submit Feedback
function submitFeedback() {
    const submitBtn = document.getElementById("submitBtn");
    submitBtn.disabled = true;

    const bookingId = document.getElementById("bookingId").value;
    const rating = document.querySelector("input[name='rating']:checked");
    const description = document.querySelector("textarea[name='description']").value.trim();

    if (!rating) {
        alert("Vui lòng chọn số sao đánh giá.");
        submitBtn.disabled = false;
        return;
    }

    const url = `${ctx}/feedback-user?action=add`;

    const data = new URLSearchParams({
        bookingId: bookingId,
        rating: rating.value,
        description: description
    });

    fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: data
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Có lỗi xảy ra.");
            }
            return res.text(); // hoặc res.json() nếu servlet trả JSON
        })
        .then(() => {
            alert("Đánh giá thành công!");
            window.location.href = `${ctx}/booking?action=history`;
        })
        .catch(err => {
            alert(err.message);
            submitBtn.disabled = false;
        });
}

// Submit Edit Feedback
function submitEditFeedback() {
    const submitBtn = document.getElementById("submitBtn");
    submitBtn.disabled = true;

    const feedbackId = document.getElementById("feedbackId").value;
    const rating = document.querySelector("input[name='rating']:checked");
    const description = document.querySelector("textarea[name='description']").value.trim();

    if (!rating) {
        alert("Vui lòng chọn số sao đánh giá.");
        submitBtn.disabled = false;
        return;
    }

    fetch(`${ctx}/feedback-user?action=update`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            feedbackId: feedbackId,
            rating: rating.value,
            description: description
        })
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Có lỗi xảy ra khi cập nhật đánh giá.");
            }
            return res.text(); // hoặc res.json() nếu servlet trả JSON
        })
        .then(() => {
            alert("Cập nhật đánh giá thành công!");
            window.location.href = `${ctx}/booking?action=history`;
        })
        .catch(err => {
            alert(err.message);
            submitBtn.disabled = false;
        });
}