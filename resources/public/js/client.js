const url = new URL(window.location.href);
const id = url.pathname.split('/')[2];

const socket = new WebSocket('http://localhost:3000/echo?id=' + id);

socket.addEventListener("open", (event) => {
  socket.send("Hello Server!");
});

socket.addEventListener("message", (event) => {
  const el = document.getElementsByClassName("alert")[0];
  el.textContent = event.data;
  el.classList.remove("d-none");
  console.log("Message from server: ", event.data);
});
