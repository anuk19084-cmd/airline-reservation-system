function searchFlights() {

    fetch("/api/flights")
        .then(response => response.json())
        .then(flights => {

            let output = "<h2>Available Flights</h2>";

            flights.forEach(flight => {

                output += `
                    <div class="flight">
                        <h3>${flight.airline}</h3>
                        <p>${flight.origin} → ${flight.destination}</p>
                        <p>Departure: ${flight.departure_time}</p>
                        <p>Arrival: ${flight.arrival_time}</p>
                        <p>Price: &#8377;${flight.price}</p>

                        <button onclick="bookFlight('${flight.airline}')">
                            Book Now
                        </button>
                    </div>
                `;
            });

            document.getElementById("flightResults").innerHTML = output;
        })
        .catch(error => {
            alert("Could not load flights!");
            console.log(error);
        });
}


function bookFlight(airline) {

    localStorage.setItem("selectedAirline", airline);

    window.location.href = "booking.html";
}
