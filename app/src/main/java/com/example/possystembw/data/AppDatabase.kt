package com.example.possystembw.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.possystembw.DAO.ARDao
import com.example.possystembw.DAO.AnnouncementsDao
import com.example.possystembw.DAO.BarcodesDao
import com.example.possystembw.DAO.CartDao
import com.example.possystembw.DAO.CashfundDao
import com.example.possystembw.DAO.CategoryDao
import com.example.possystembw.DAO.ControlsDao
import com.example.possystembw.DAO.CustomerDao
import com.example.possystembw.DAO.DetailsDao
import com.example.possystembw.DAO.DiscountDao
import com.example.possystembw.DAO.ImportproductsDao
import com.example.possystembw.DAO.InventitembarcodesDao
import com.example.possystembw.DAO.InventjournaltablesDao
import com.example.possystembw.DAO.InventjournaltransDao
import com.example.possystembw.DAO.InventjournaltransreposDao
import com.example.possystembw.DAO.InventtablemodulesDao
import com.example.possystembw.DAO.InventtablesDao
import com.example.possystembw.DAO.ItemsDao
import com.example.possystembw.DAO.MixMatchDao
import com.example.possystembw.DAO.NumbersequencetablesDao
import com.example.possystembw.DAO.NumbersequencevaluesDao
import com.example.possystembw.DAO.PartycakesDao
import com.example.possystembw.DAO.ProductDao
import com.example.possystembw.DAO.RboinventablesDao
import com.example.possystembw.DAO.RboinventitemretailgroupsDao
import com.example.possystembw.DAO.RbospecialgroupsDao
import com.example.possystembw.DAO.RbostoretablesDao
import com.example.possystembw.DAO.SptablesDao
import com.example.possystembw.DAO.SptransDao
import com.example.possystembw.DAO.SptransreposDao
import com.example.possystembw.DAO.TenderDeclarationDao
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.TransactionSummaryDao
import com.example.possystembw.DAO.TxtfileDao
import com.example.possystembw.DAO.UserDao
import com.example.possystembw.DAO.WindowDao
import com.example.possystembw.DAO.WindowTableDao
import com.example.possystembw.DAO.RboTransactionDiscountTransDao
import com.example.possystembw.DAO.ZReadDao
import com.example.possystembw.DAO.NumberSequenceDao
import com.example.possystembw.database.AR
import com.example.possystembw.database.Announcements
import com.example.possystembw.database.Barcodes
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.Cashfund
import com.example.possystembw.database.Controls
import com.example.possystembw.database.Details
import com.example.possystembw.database.Importproducts
import com.example.possystembw.database.Inventitembarcodes
import com.example.possystembw.database.Inventjournaltables
import com.example.possystembw.database.Inventjournaltrans
import com.example.possystembw.database.Inventjournaltransrepos
import com.example.possystembw.database.Inventtablemodules
import com.example.possystembw.database.Inventtables
import com.example.possystembw.database.Items
import com.example.possystembw.database.Numbersequencetables
import com.example.possystembw.database.Numbersequencevalues
import com.example.possystembw.database.Partycakes
import com.example.possystembw.database.Product
import com.example.possystembw.database.Rboinventables
import com.example.possystembw.database.Rboinventitemretailgroups
import com.example.possystembw.database.Rbospecialgroups
import com.example.possystembw.database.Rbostoretables
import com.example.possystembw.database.Sptables
import com.example.possystembw.database.Sptrans
import com.example.possystembw.database.Sptransrepos
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.database.Txtfile
import com.example.possystembw.database.User
import com.example.possystembw.database.Category
import com.example.possystembw.database.Customer
import com.example.possystembw.database.Window
import com.example.possystembw.database.WindowTable
import com.example.possystembw.database.Discount
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.RboTransactionDiscountTrans
import com.example.possystembw.database.ProductBundle
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchLineGroup
import com.example.possystembw.database.MixMatchDiscountLine
import com.example.possystembw.database.ZRead
import com.example.possystembw.database.NumberSequenceEntity
import com.example.possystembw.database.NumberSequence





import com.example.possystembw.ui.ViewModel.Converters

@Database(entities = [Product::class,
                      CartItem::class,
                      TransactionRecord::class,TransactionSummary::class,
                      Rboinventables::class,
                      Inventtables::class,
                      Inventtablemodules::class,
                      Announcements::class, Barcodes::class, Controls::class,
                      Details::class, Importproducts::class,
                      Inventitembarcodes::class, Inventjournaltables::class,
                      Inventjournaltrans::class, Inventjournaltransrepos::class,
                      Items::class, Numbersequencetables::class,
                      Numbersequencevalues::class, Partycakes::class,
                      Rboinventitemretailgroups::class, Rbospecialgroups::class,
                      Rbostoretables::class, Sptables::class,
                       Sptrans::class, Sptransrepos::class,
                       Txtfile::class,User::class, Cashfund::class,
                        Category::class,Window::class,WindowTable::class,Discount::class,
                        Customer::class, AR::class,TenderDeclaration::class,
                    RboTransactionDiscountTrans::class,ProductBundle::class, MixMatch::class,
    MixMatchLineGroup::class,
    MixMatchDiscountLine::class,ZRead::class,NumberSequenceEntity::class,NumberSequence::class],
    version = 121)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun transactionDao(): TransactionDao
    abstract fun rboinventablesDao(): RboinventablesDao
    abstract fun inventtablesDao(): InventtablesDao
    abstract fun inventtablemdulesDao(): InventtablemodulesDao
    abstract fun announcementsDao(): AnnouncementsDao
    abstract fun barcodesDao(): BarcodesDao
    abstract fun controlsDao(): ControlsDao
    abstract fun detailsDao(): DetailsDao
    abstract fun importproductsDao(): ImportproductsDao
    abstract fun inventitembarcodesDao(): InventitembarcodesDao
    abstract fun inventjournaltablesDao(): InventjournaltablesDao
    abstract fun inventjournaltransDao(): InventjournaltransDao // Add this line
    abstract fun inventjournaltransreposDao(): InventjournaltransreposDao
    abstract fun itemsDao(): ItemsDao // Add this line
    abstract fun numbersequencetablesDao(): NumbersequencetablesDao
    abstract fun numbersequencevaluesDao(): NumbersequencevaluesDao // Add this line
    abstract fun partycakesDao(): PartycakesDao
    abstract fun rboinventitemretailgroupsDao(): RboinventitemretailgroupsDao
    abstract fun rbospecialgroupsDao(): RbospecialgroupsDao
    abstract fun rbostoretablesDao(): RbostoretablesDao // Add this line
    abstract fun sptablesDao(): SptablesDao //
    abstract fun sptransDao(): SptransDao// Add this line
    abstract fun sptransreposDao(): SptransreposDao
    abstract fun txtfileDao(): TxtfileDao // Add this line
    abstract fun userDao(): UserDao
    abstract fun cashFundDao(): CashfundDao
    abstract fun categoryDao(): CategoryDao
    abstract fun windowDao(): WindowDao
    abstract fun windowTableDao(): WindowTableDao
    abstract fun discountDao(): DiscountDao
    abstract fun transactionSummaryDao(): TransactionSummaryDao
    abstract fun customerDao(): CustomerDao
    abstract fun arDao(): ARDao
    abstract fun tenderDeclarationDao(): TenderDeclarationDao
    abstract fun rboTransactionDiscountTransDao(): RboTransactionDiscountTransDao
    abstract fun mixMatchDao(): MixMatchDao
    abstract fun zReadDao(): ZReadDao
    abstract fun numberSequenceDao(): NumberSequenceDao












    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d(TAG, "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "POSBWbakeshop92" // Keep this new name if you want to start fresh
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,MIGRATION_9_10,
                        MIGRATION_10_11,MIGRATION_11_12,MIGRATION_12_13,MIGRATION_13_14,MIGRATION_14_15,
                        MIGRATION_15_16,MIGRATION_16_17,MIGRATION_17_18,MIGRATION_18_19,
                        MIGRATION_19_20,MIGRATION_20_21,MIGRATION_21_22,MIGRATION_22_23,
                        MIGRATION_23_24,MIGRATION_24_25,MIGRATION_25_26,MIGRATION_26_27,
                        MIGRATION_27_28,MIGRATION_28_29,MIGRATION_29_30,MIGRATION_30_31,
                        MIGRATION_31_32,MIGRATION_32_33,MIGRATION_33_34,MIGRATION_34_35,
                        MIGRATION_35_36,MIGRATION_36_37,MIGRATION_37_38
                    )
                    .build()
                Log.d(TAG, "Database version: ${instance.openHelper.readableDatabase.version}") // Log version here
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `products` ADD COLUMN `activeondelivery` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `products` ADD COLUMN `itemgroup` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `products` ADD COLUMN `specialgroup` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `products` ADD COLUMN `production` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `products` ADD COLUMN `moq` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `products` ADD COLUMN `barcode` INTEGER NOT NULL DEFAULT 0")

                // Step 2: Update existing records with new default values
                database.execSQL("UPDATE `products` SET activeondelivery = 0")  // Default value for new column
                database.execSQL("UPDATE `products` SET itemgroup = ''")        // Set defaults for new columns
                database.execSQL("UPDATE `products` SET specialgroup = ''")
                database.execSQL("UPDATE `products` SET production = ''")
                database.execSQL("UPDATE `products` SET moq = 0")
                database.execSQL("UPDATE `products` SET barcode = 0")

                // Optionally, you can also update existing price and cost to be rounded if needed
                database.execSQL("UPDATE `products` SET price = ROUND(price, 2)")
                database.execSQL("UPDATE `products` SET cost = ROUND(cost, 2)")

            }
        }

        // Add placeholder migrations for versions 2 to 5
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update existing columns with new data types or default values
                // Example of updating the price and cost to new defaults
                database.execSQL(
                    """
            UPDATE `products`
            SET 
                price = ROUND(price, 2),  -- Adjust price to 2 decimal places
                cost = ROUND(cost, 2),    -- Adjust cost to 2 decimal places
                activeondelivery = 0,      -- Set default value for new column
                itemgroup = '',            -- Set default for new columns
                specialgroup = '',
                production = '',
                moq = 0,
                barcode = 0
            """
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration steps if necessary
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration steps if necessary
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration steps if necessary
            }
        }



        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN subtotal REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN vat_rate REAL NOT NULL DEFAULT 0.12")
                database.execSQL("ALTER TABLE transactions ADD COLUMN vat_amount REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN discount_rate REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN discount_amount REAL NOT NULL DEFAULT 0.0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN image_url TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any necessary changes for version 9
                // If there are no changes, you can leave this empty
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `rboinventables` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `itemid` TEXT NOT NULL,
                `itemtype` TEXT NOT NULL,
                `itemgroup` TEXT NOT NULL,
                `itemdepartment` TEXT NOT NULL,
                `zeropricevalid` INTEGER NOT NULL,
                `dateblocked` INTEGER,
                `datetobeblocked` INTEGER,
                `blockedonpos` INTEGER NOT NULL,
                `Activeondelivery` INTEGER NOT NULL,
                `barcode` TEXT NOT NULL,
                `datetoactivateitem` INTEGER,
                `mustselectuom` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `PRODUCTION` TEXT NOT NULL,
                `moq` INTEGER NOT NULL,
                `fgcount` INTEGER NOT NULL,
                `TRANSPARENTSTOCKS` INTEGER NOT NULL,
                `stocks` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventtables` (
                `itemGroupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `itemid` TEXT NOT NULL,
                `itemname` TEXT NOT NULL,
                `itemtype` TEXT NOT NULL,
                `namealias` TEXT NOT NULL,
                `notes` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventtablemodules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `itemid` TEXT NOT NULL,
                `moduletype` TEXT NOT NULL,
                `unitid` TEXT NOT NULL,
                `price` REAL NOT NULL,
                `priceunit` TEXT NOT NULL,
                `priceincltax` INTEGER NOT NULL,
                `quantity` INTEGER NOT NULL,
                `lowestqty` INTEGER NOT NULL,
                `highestqty` INTEGER NOT NULL,
                `blocked` INTEGER NOT NULL,
                `inventlocationid` TEXT NOT NULL,
                `pricedate` INTEGER NOT NULL,
                `taxitemgroupid` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `announcements` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `subject` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `file_path` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `barcodes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `barcode` TEXT NOT NULL,
                `description` TEXT,
                `isuse` INTEGER NOT NULL,
                `generateby` TEXT NOT NULL,
                `generatedate` INTEGER NOT NULL,
                `modifiedby` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `controls` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `description` TEXT,
                `datecreated` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `details` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `fgencoder` TEXT NOT NULL,
                `plencoder` TEXT NOT NULL,
                `dispatcher` TEXT NOT NULL,
                `logistics` TEXT NOT NULL,
                `routes` TEXT NOT NULL,
                `createddate` INTEGER NOT NULL,
                `deliverydate` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `importproducts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `itemid` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `searchalias` TEXT NOT NULL,
                `notes` TEXT,
                `retailgroup` TEXT NOT NULL,
                `retaildepartment` TEXT NOT NULL,
                `salestaxgroup` TEXT NOT NULL,
                `costprice` REAL NOT NULL,
                `salesprice` REAL NOT NULL,
                `barcodesetup` TEXT NOT NULL,
                `barcode` TEXT NOT NULL,
                `barcodeunit` TEXT NOT NULL,
                `activestatus` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventitembarcodes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ITEMBARCODE` TEXT NOT NULL,
                `ITEMID` TEXT NOT NULL,
                `BARCODESETUPID` TEXT NOT NULL,
                `DESCRIPTION` TEXT NOT NULL,
                `QTY` INTEGER NOT NULL,
                `UNITID` TEXT NOT NULL,
                `RBOVARIANTID` TEXT NOT NULL,
                `BLOCKED` INTEGER NOT NULL,
                `MODIFIEDBY` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventjournaltables` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `JOURNALID` TEXT NOT NULL,
                `DESCRIPTION` TEXT NOT NULL,
                `POSTED` INTEGER NOT NULL,
                `POSTEDDATETIME` INTEGER NOT NULL,
                `JOURNALTYPE` TEXT NOT NULL,
                `DELETEPOSTEDLINES` INTEGER NOT NULL,
                `CREATEDDATETIME` INTEGER NOT NULL,
                `STOREID` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `OPICPOSTED` INTEGER NOT NULL,
                `FGENCODER` TEXT NOT NULL,
                `PLENCODER` TEXT NOT NULL,
                `DISPATCHER` TEXT NOT NULL,
                `LOGISTICS` TEXT NOT NULL,
                `DELIVERYDATE` INTEGER NOT NULL,
                `orangecrates` INTEGER NOT NULL,
                `bluecrates` INTEGER NOT NULL,
                `empanadacrates` INTEGER NOT NULL,
                `box` INTEGER NOT NULL,
                `sent` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventjournaltrans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `JOURNALID` TEXT NOT NULL,
                `LINENUM` INTEGER NOT NULL,
                `TRANSDATE` INTEGER NOT NULL,
                `ITEMID` TEXT NOT NULL,
                `ADJUSTMENT` REAL NOT NULL,
                `COSTPRICE` REAL NOT NULL,
                `PRICEUNIT` TEXT NOT NULL,
                `SALESAMOUNT` REAL NOT NULL,
                `INVENTONHAND` INTEGER NOT NULL,
                `COUNTED` INTEGER NOT NULL,
                `REASONREFRECID` TEXT NOT NULL,
                `VARIANTID` TEXT NOT NULL,
                `POSTED` INTEGER NOT NULL,
                `POSTEDDATETIME` INTEGER NOT NULL,
                `UNITID` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `MGCOUNT` INTEGER NOT NULL,
                `BALANCECOUNT` INTEGER NOT NULL,
                `CHECKINGCOUNT` INTEGER NOT NULL,
                `itemdepartment` TEXT NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `inventjournaltransrepos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `JOURNALID` TEXT NOT NULL,
                `LINENUM` INTEGER NOT NULL,
                `TRANSDATE` INTEGER NOT NULL,
                `ITEMID` TEXT NOT NULL,
                `COUNTED` INTEGER NOT NULL,
                `STORENAME` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `moq` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `itemdepartment` TEXT NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `price` REAL NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `numbersequencetables` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `NUMBERSEQUENCE` TEXT NOT NULL,
                `TXT` TEXT NOT NULL,
                `LOWEST` INTEGER NOT NULL,
                `HIGHEST` INTEGER NOT NULL,
                `BLOCKED` INTEGER NOT NULL,
                `STOREID` INTEGER NOT NULL,
                `CANBEDELETED` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `numbersequencevalues` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `NUMBERSEQUENCE` TEXT NOT NULL,
                `NEXTREC` INTEGER NOT NULL,
                `STOREID` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `partycakes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `COSNO` TEXT NOT NULL,
                `BRANCH` TEXT NOT NULL,
                `DATEORDER` INTEGER NOT NULL,
                `CUSTOMERNAME` TEXT NOT NULL,
                `ADDRESS` TEXT NOT NULL,
                `TELNO` TEXT NOT NULL,
                `DATEPICKEDUP` INTEGER,
                `TIMEPICKEDUP` TEXT,
                `DELIVERED` INTEGER NOT NULL,
                `TIMEDELIVERED` TEXT,
                `DEDICATION` TEXT,
                `BDAYCODENO` TEXT,
                `FLAVOR` TEXT,
                `MOTIF` TEXT,
                `ICING` TEXT,
                `OTHERS` TEXT,
                `SRP` REAL NOT NULL,
                `DISCOUNT` REAL NOT NULL,
                `PARTIALPAYMENT` REAL NOT NULL,
                `NETAMOUNT` REAL NOT NULL,
                `BALANCEAMOUNT` REAL NOT NULL,
                `STATUS` TEXT NOT NULL,
                `file_path` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `TRANSACTSTORE` TEXT NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `rboinventitemretailgroups` (
                `groupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `NAME` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `rbospecialgroups` (
                `groupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `NAME` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `rbostoretables` (
                `storeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `NAME` TEXT NOT NULL,
                `ADDRESS` TEXT NOT NULL,
                `STREET` TEXT NOT NULL,
                `ZIPCODE` TEXT NOT NULL,
                `CITY` TEXT NOT NULL,
                `STATE` TEXT NOT NULL,
                `COUNTRY` TEXT NOT NULL,
                `PHONE` TEXT NOT NULL,
                `CURRENCY` TEXT NOT NULL,
                `SQLSERVERNAME` TEXT NOT NULL,
                `DATABASENAME` TEXT NOT NULL,
                `USERNAME` TEXT NOT NULL,
                `PASSWORD` TEXT NOT NULL,
                `WINDOWSAUTHENTICATION` INTEGER NOT NULL,
                `FORMINFOFIELD1` TEXT,
                `FORMINFOFIELD2` TEXT,
                `FORMINFOFIELD3` TEXT,
                `FORMINFOFIELD4` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `ROUTES` TEXT,
                `TYPES` TEXT,
                `BLOCKED` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `sptables` (
                `journalId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `DESCRIPTION` TEXT NOT NULL,
                `POSTED` INTEGER NOT NULL,
                `POSTEDDATETIME` INTEGER NOT NULL,
                `JOURNALTYPE` TEXT NOT NULL,
                `DELETEPOSTEDLINES` INTEGER NOT NULL,
                `CREATEDDATETIME` INTEGER NOT NULL,
                `STOREID` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `sptrans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `JOURNALID` INTEGER NOT NULL,
                `STORENAME` TEXT NOT NULL,
                `TRANSDATE` INTEGER NOT NULL,
                `ITEMID` TEXT NOT NULL,
                `ADJUSTMENT` REAL NOT NULL,
                `COSTPRICE` REAL NOT NULL,
                `PRICEUNIT` REAL NOT NULL,
                `SALESAMOUNT` REAL NOT NULL,
                `INVENTONHAND` INTEGER NOT NULL,
                `COUNTED` INTEGER NOT NULL,
                `REASONREFRECID` TEXT,
                `VARIANTID` TEXT,
                `POSTED` INTEGER NOT NULL,
                `POSTEDDATETIME` INTEGER NOT NULL,
                `UNITID` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `COUNT` INTEGER NOT NULL,
                `REMARKS` TEXT
            )
            """
                )
            }
        }
        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `sptransrepos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `JOURNALID` INTEGER NOT NULL,
                `LINENUM` INTEGER NOT NULL,
                `TRANSDATE` INTEGER NOT NULL,
                `ITEMID` TEXT NOT NULL,
                `COUNTED` INTEGER NOT NULL,
                `STORENAME` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `txtfile` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `BRANCH` TEXT NOT NULL,
                `FILENAME` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `logins` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `storied` INTEGER NOT NULL,
                `email_verified_at` INTEGER,
                `password` TEXT NOT NULL,
                `two_factor_secret` TEXT,
                `two_factor_recovery_codes` TEXT,
                `two_factor_confirmed_at` INTEGER,
                `remember_token` TEXT,
                `current_team_id` TEXT,
                `profile_photo_path` TEXT,
                `role` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """
                )
            }
        }
        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN window_number INTEGER NOT NULL DEFAULT 1") // Default to 1
            }
        }
        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new column to the existing table
                database.execSQL("ALTER TABLE cart_items ADD COLUMN window_id INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    """CREATE TABLE cash_fund (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cash_fund REAL NOT NULL,
                status TEXT NOT NULL,
                date TEXT NOT NULL
            )"""
                )
            }
        }
        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new category table
                database.execSQL("""
            CREATE TABLE `category` (
                `groupId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL
            )
        """)
            }
        }
        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create Window table
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `windows` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """
                )
            }
        }
        val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the windowtable with 'description' instead of 'name'
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS `windowtable` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `description` TEXT NOT NULL
                
            )
        """.trimIndent())
            }
        }



    }
}
