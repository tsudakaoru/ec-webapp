const cartCount = document.querySelector(".cart-count");
const cartLink = document.querySelector(".cart-link");
const toast = document.querySelector(".toast");
let toastTimer;

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle("is-error", isError);
  toast.classList.add("is-visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 3000);
}

document.querySelectorAll(".add-to-cart-form").forEach((form) => {
  form.addEventListener("submit", () => {
    const button = form.querySelector(".add-to-cart");
    button.disabled = true;
    button.textContent = "カートに追加中";
  });
});

