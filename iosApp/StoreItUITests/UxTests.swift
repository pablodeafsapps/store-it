import XCTest

final class UxTests: XCTestCase {
    
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
        // For the launch-performance test, avoid launching the app here (it is launched inside the `measure` block).
        if self.name.contains("testLaunchPerformance") == false {
            app = XCUIApplication()
            app.launchArguments = ["isRunningUITests"] // Optional: enable test mode
            app.launch()
        }
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    @MainActor
    func testLaunchPerformance() throws {
        // This measures how long it takes to launch your application.
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            XCUIApplication().launch()
        }
    }
    
    @MainActor
    func testAddRackButtonExistsAndTaps() {
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()
        
        let addRackTitle = app.staticTexts["Add Rack"]
        XCTAssertTrue(addRackTitle.exists)
    }

    @MainActor
    func testAddRackScreenCancelReturnsToList() {
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let addRackTitle = app.staticTexts["Add Rack"]
        XCTAssertTrue(addRackTitle.waitForExistence(timeout: 5))

        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.waitForExistence(timeout: 5))
        cancelButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))
    }

    @MainActor
    func testAddRackSaveCreatesRackAndShowsInList() {
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Test Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 5))
        saveButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
    }

    @MainActor
    func testRackDetailBackNavigation() {
        // Ensure there is at least one rack by creating it via the Add Rack flow
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Detail Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.exists)
        saveButton.tap()

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let detailTitle = app.navigationBars["Detail Rack"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 5))

        let backButton = detailTitle.buttons.element(boundBy: 0)
        XCTAssertTrue(backButton.waitForExistence(timeout: 5))
        backButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))
    }

    @MainActor
    func testRackDetailEditSheetAndDeleteAlert() {
        // Create a rack to open its detail
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Editable Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.exists)
        saveButton.tap()

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let menuButton = app.buttons["rackDetailMenuButton"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 5))
        menuButton.tap()

        let editButton = app.buttons["Edit"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 5))
        editButton.tap()

        let editSaveButton = app.buttons["Save"]
        let editCancelButton = app.buttons["Cancel"]
        XCTAssertTrue(editSaveButton.waitForExistence(timeout: 5))
        XCTAssertTrue(editCancelButton.waitForExistence(timeout: 5))

        editCancelButton.tap()

        // Open delete flow and cancel
        menuButton.tap()

        let removeButton = app.buttons["Remove rack"]
        XCTAssertTrue(removeButton.waitForExistence(timeout: 5))
        removeButton.tap()

        let deleteAlert = app.alerts["Remove rack?"]
        XCTAssertTrue(deleteAlert.waitForExistence(timeout: 5))

        let cancelDeleteButton = deleteAlert.buttons["Cancel"]
        XCTAssertTrue(cancelDeleteButton.exists)
        cancelDeleteButton.tap()
    }

    @MainActor
    func testRackDetailUsesAccessibilityIdentifiersForMenuAndEditSheet() {
        // Create a rack to open its detail
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("A11y Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 5))
        saveButton.tap()

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let menuButton = app.buttons["rackDetailMenuButton"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 5))
        menuButton.tap()

        let editMenuItem = app.buttons["editRackMenuItem"]
        let removeMenuItem = app.buttons["removeRackMenuItem"]
        XCTAssertTrue(editMenuItem.waitForExistence(timeout: 5))
        XCTAssertTrue(removeMenuItem.waitForExistence(timeout: 5))

        editMenuItem.tap()

        let editRackCancel = app.buttons["editRackCancelButton"]
        let editRackSave = app.buttons["editRackSaveButton"]
        XCTAssertTrue(editRackCancel.waitForExistence(timeout: 5))
        XCTAssertTrue(editRackSave.waitForExistence(timeout: 5))

        editRackCancel.tap()
    }

    @MainActor
    func testRackDetailTapExistingSlotShowsItemsSheet() {
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Slot Tap Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.exists)
        saveButton.tap()

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let detailTitle = app.navigationBars["Slot Tap Rack"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 10))

        let otherImageArea = app.otherElements["rackDetailImageArea"]
        if otherImageArea.waitForExistence(timeout: 10) {
            otherImageArea.tap()
        } else {
            let imageAreaAsImage = app.images["rackDetailImageArea"]
            XCTAssertTrue(imageAreaAsImage.waitForExistence(timeout: 10))
            imageAreaAsImage.tap()
        }

        let cancelAddItem = app.buttons["Cancel"]
        XCTAssertTrue(cancelAddItem.waitForExistence(timeout: 5))
        cancelAddItem.tap()

        XCTAssertTrue(app.navigationBars["Slot Tap Rack"].waitForExistence(timeout: 5))

        let imageAreaAgain = app.otherElements["rackDetailImageArea"]
        XCTAssertTrue(imageAreaAgain.waitForExistence(timeout: 5))
        imageAreaAgain.tap()

        let sheetTitle = app.staticTexts["Items in this slot"]
        XCTAssertTrue(sheetTitle.waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["No items stored here."].exists)

        let slotItemsNavBar = app.navigationBars["Items in this slot"]
        let backFromSlotItems = slotItemsNavBar.buttons.element(boundBy: 0)
        XCTAssertTrue(backFromSlotItems.waitForExistence(timeout: 5))
        backFromSlotItems.tap()
    }

    @MainActor
    func testRackDetailLongPressAndDragSlotShowsMoveConfirmation() {
        let addRackButton = app.buttons["Add Rack"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Slot Drag Rack")

        let saveButton = app.buttons["Save Rack"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 5))
        saveButton.tap()

        let rackCell = app.buttons["rackRowViewButton"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let detailTitle = app.navigationBars["Slot Drag Rack"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 10))

        let imageArea = rackDetailImageArea()
        XCTAssertTrue(imageArea.waitForExistence(timeout: 10))

        let slotStart = imageArea.coordinate(withNormalizedOffset: CGVector(dx: 0.30, dy: 0.35))
        slotStart.tap()

        let cancelAddItem = app.buttons["Cancel"]
        XCTAssertTrue(cancelAddItem.waitForExistence(timeout: 5))
        cancelAddItem.tap()

        XCTAssertTrue(detailTitle.waitForExistence(timeout: 5))

        let slotDragEnd = imageArea.coordinate(withNormalizedOffset: CGVector(dx: 0.55, dy: 0.55))
        slotStart.press(forDuration: 0.6, thenDragTo: slotDragEnd)

        let moveAlert = app.alerts["Move slot marker?"]
        XCTAssertTrue(moveAlert.waitForExistence(timeout: 5))

        let cancelMoveButton = moveAlert.buttons["Cancel"]
        XCTAssertTrue(cancelMoveButton.waitForExistence(timeout: 5))
        cancelMoveButton.tap()
    }

    @MainActor
    private func rackDetailImageArea() -> XCUIElement {
        let imageArea = app.otherElements["rackDetailImageArea"]
        if imageArea.exists {
            return imageArea
        }

        return app.images["rackDetailImageArea"]
    }
}
