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
  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = form.querySelector(".add-to-cart");
    const currentQuantity = Number(form.dataset.currentQuantity);
    const stock = Number(form.dataset.stock);
    button.disabled = true;

    try {
      const response = await fetch(form.action, {
        method: "POST",
        body: new FormData(form),
        headers: { "X-Requested-With": "XMLHttpRequest" }
      });
      const responseText = await response.text();

      if (!response.ok) {
        button.textContent = "追加不可";
        showToast(responseText, true);
        return;
      }

      const updatedQuantity = currentQuantity + 1;
      form.dataset.currentQuantity = String(updatedQuantity);
      cartCount.textContent = responseText;
      cartLink.setAttribute("aria-label", `カートを見る。現在${responseText}点`);

      if (updatedQuantity >= stock) {
        button.textContent = "追加済み";
      } else {
        button.disabled = false;
      }
      showToast(`${form.dataset.productName}を1点カートに追加しました`);
    } catch {
      button.disabled = false;
      showToast("カートに追加できませんでした。時間をおいて再度お試しください。", true);
    }
  });
});

