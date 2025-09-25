package com.example.vetfinance.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

/**
 * Representa una transacción de venta única en la base de datos.
 *
 * @property saleId El identificador único para la venta, generado automáticamente.
 * @property clientIdFk La clave foránea que vincula la venta con el [Client] que la realizó.
 * @property totalAmount El monto total de la venta.
 * @property date La fecha y hora en que se realizó la venta, almacenada como milisegundos.
 */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["clientId"],
            childColumns = ["clientIdFk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clientIdFk")]
)
data class Sale(
    @PrimaryKey
    val saleId: String = UUID.randomUUID().toString(),
    val clientIdFk: String,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis()
)

/**
 * Clase de relación que une una [Sale] con la lista de [Product]s que se vendieron en ella.
 * Room utiliza esta clase para cargar una venta completa con todos sus detalles en una sola consulta.
 *
 * @property sale La entidad de la venta.
 * @property products La lista de productos asociados a esa venta.
 */
data class SaleWithProducts(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "saleId",
        entityColumn = "id",
        associateBy = Junction(
            value = SaleProductCrossRef::class,
            parentColumn = "saleId",
            entityColumn = "productId"
        )
    )
    val products: List<Product>
)

/**
 * Tabla de referencia cruzada (join table) para la relación muchos-a-muchos entre [Sale] y [Product].
 * Cada entrada en esta tabla representa un artículo de línea en una venta.
 *
 * @property saleId La clave foránea que apunta a la [Sale].
 * @property productId La clave foránea que apunta al [Product].
 * @property quantity La cantidad de este producto que se vendió en esta venta.
 * @property priceAtTimeOfSale El precio del producto en el momento exacto de la venta, para mantener un registro histórico preciso.
 */
@Entity(
    tableName = "sales_products_cross_ref",
    primaryKeys = ["saleId", "productId"],
    indices = [Index(value = ["productId"])]
)
data class SaleProductCrossRef(
    val saleId: String,
    val productId: String,
    val quantity: Int,
    val priceAtTimeOfSale: Double
)