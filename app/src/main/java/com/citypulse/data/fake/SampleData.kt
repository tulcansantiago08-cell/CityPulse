package com.citypulse.data.fake

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Datos de ejemplo de Madrid, los mismos del prototipo, anclados al día actual. */
object SampleData {

    val Madrid = City("Madrid", "España", GeoPoint(40.4168, -3.7038))

    /** Punto de referencia del usuario en los datos de ejemplo. */
    val PuertaDelSol = GeoPoint(40.4169, -3.7035)

    private fun unsplash(id: String) = "https://images.unsplash.com/$id?auto=format&fit=crop&w=720&q=70"

    private fun at(day: LocalDate, time: String): LocalDateTime =
        if (time == "24:00") day.plusDays(1).atStartOfDay() else day.atTime(LocalTime.parse(time))

    fun events(today: LocalDate = LocalDate.now()): List<Event> {
        fun event(
            id: String,
            title: String,
            category: Category,
            venue: String,
            area: String,
            location: GeoPoint,
            dayOffset: Long,
            from: String,
            to: String,
            price: String?,
            image: String,
            description: String,
        ): Event {
            val day = today.plusDays(dayOffset)
            return Event(
                id = id,
                title = title,
                category = category,
                venue = venue,
                area = area,
                location = location,
                start = at(day, from),
                end = at(day, to),
                price = price,
                isFree = price == null,
                description = description,
                imageUrl = unsplash(image),
                source = EventSource.CityPulse,
            )
        }

        return listOf(
            event(
                "jazz", "Jazz de medianoche", Category.Music, "Café Central", "Plaza del Ángel",
                GeoPoint(40.4142, -3.7006), 0, "21:00", "23:30", "15 €", "photo-1415201364774-f6f0bb35f28f",
                "Cuarteto residente del club de jazz más veterano de Madrid. Repertorio de standards y composiciones propias. Mesas por orden de llegada; la segunda sesión empieza a las 22:30.",
            ),
            event(
                "slam", "Poesía slam en Lavapiés", Category.Culture, "La Escalera", "Lavapiés",
                GeoPoint(40.4110, -3.7010), 0, "20:30", "22:30", null, "photo-1478737270239-2f02b77fc618",
                "Micro abierto con poetas del barrio. Tres minutos por persona, el público vota. Apúntate en la barra si quieres leer.",
            ),
            event(
                "dj", "Sesión DJ al aire libre", Category.Music, "Matadero Madrid", "Legazpi",
                GeoPoint(40.3920, -3.6975), 0, "19:00", "24:00", null, "photo-1571266028243-e4733b0f0bb0",
                "Plaza Matadero se convierte en pista: house y disco con DJs locales hasta medianoche. Food trucks y barra en la nave 16.",
            ),
            event(
                "flamenco", "Flamenco en la Corrala", Category.Music, "Tablao La Corrala", "La Latina",
                GeoPoint(40.4100, -3.7060), 0, "22:00", "23:15", "25 €", "photo-1504609813442-a8924e83f76e",
                "Cante, guitarra y baile en un patio de corrala de 1880. Incluye una bebida. Aforo de 60 personas.",
            ),
            event(
                "sanil", "Tapeo en San Ildefonso", Category.Food, "Mercado de San Ildefonso", "Malasaña",
                GeoPoint(40.4235, -3.7020), 0, "12:00", "24:00", null, "photo-1555396273-367ea4eb4db5",
                "Tres plantas de puestos de comida callejera y una terraza interior. Entrada libre; consumo aparte.",
            ),
            event(
                "libro", "Feria del libro antiguo", Category.Fairs, "Paseo de Recoletos", "Recoletos",
                GeoPoint(40.4220, -3.6920), 0, "11:00", "21:00", null, "photo-1481627834876-b7833e8f5570",
                "Más de 30 casetas de libreros de viejo con primeras ediciones, cómics y grabados. Último día el domingo.",
            ),
            event(
                "yoga", "Yoga al amanecer", Category.Culture, "Parque del Retiro", "Retiro",
                GeoPoint(40.4180, -3.6845), 1, "08:00", "09:00", null, "photo-1544367567-0f2fcb009e0b",
                "Clase abierta junto al estanque. Trae tu esterilla; nivel principiante.",
            ),
            event(
                "sorolla", "Luz del Mediterráneo", Category.Art, "Sala Alcalá 31", "Centro",
                GeoPoint(40.4187, -3.6990), 2, "10:00", "14:00", "8 €", "photo-1518998053901-5348d3961a04",
                "Exposición de pintura luminista con visitas guiadas cada hora. Gratis los jueves a partir de las 18:00.",
            ),
            event(
                "cine", "Cine de verano", Category.Culture, "Conde Duque", "Conde Duque",
                GeoPoint(40.4275, -3.7105), 3, "22:00", "24:00", "6 €", "photo-1517604931442-7e0c8ed2963c",
                "Clásicos en versión original bajo las estrellas en el patio central. Sillas de lona incluidas.",
            ),
            event(
                "motores", "Mercado de Motores", Category.Markets, "Museo del Ferrocarril", "Delicias",
                GeoPoint(40.3985, -3.6940), 4, "11:00", "22:00", null, "photo-1488459716781-31db52582fe9",
                "Diseño, vintage y food trucks entre trenes históricos. Música en directo por la tarde.",
            ),
            event(
                "rastro", "El Rastro", Category.Markets, "Ribera de Curtidores", "La Latina",
                GeoPoint(40.4085, -3.7070), 5, "09:00", "15:00", null, "photo-1533900298318-6b8da08a523e",
                "El mercadillo al aire libre más famoso de la ciudad: antigüedades, ropa y discos. Ve temprano para evitar las multitudes.",
            ),
        )
    }
}
