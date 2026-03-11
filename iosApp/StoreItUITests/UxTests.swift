import XCTest

final class UxTests: XCTestCase {
    
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
        app = XCUIApplication()
        app.launchArguments = ["isRunningUITests"] // Optional: enable test mode
        app.launch()
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
        let addRackButton = app.buttons["addRackButton"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()
        
        let addRackTitle = app.staticTexts["Add Rack"]
        XCTAssertTrue(addRackTitle.exists)
    }

    @MainActor
    func testAddRackScreenCancelReturnsToList() {
        let addRackButton = app.buttons["addRackButton"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let addRackTitle = app.staticTexts["Add Rack"]
        XCTAssertTrue(addRackTitle.waitForExistence(timeout: 5))

        let cancelButton = app.buttons["cancelButton"]
        XCTAssertTrue(cancelButton.exists)
        cancelButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))
    }

    @MainActor
    func testAddRackSaveCreatesRackAndShowsInList() {
        let addRackButton = app.buttons["addRackButton"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Test Rack")

        let saveButton = app.buttons["saveRackButton"]
        XCTAssertTrue(saveButton.waitForExistence(timeout: 5))
        saveButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))

        let rackCell = app.staticTexts["Test Rack"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
    }

    @MainActor
    func testRackDetailBackNavigation() {
        // Ensure there is at least one rack by creating it via the Add Rack flow
        let addRackButton = app.buttons["addRackButton"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Detail Rack")

        let saveButton = app.buttons["saveRackButton"]
        XCTAssertTrue(saveButton.exists)
        saveButton.tap()

        let rackCell = app.staticTexts["Detail Rack"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let detailTitle = app.navigationBars["Detail Rack"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 5))

        let backButton = app.buttons["rackDetailBackButton"]
        XCTAssertTrue(backButton.exists)
        backButton.tap()

        let racksTitle = app.navigationBars["Racks"]
        XCTAssertTrue(racksTitle.waitForExistence(timeout: 5))
    }

    @MainActor
    func testRackDetailEditSheetAndDeleteAlert() {
        // Create a rack to open its detail
        let addRackButton = app.buttons["addRackButton"]
        XCTAssertTrue(addRackButton.waitForExistence(timeout: 5))
        addRackButton.tap()

        let nameField = app.textFields["Name *"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 5))
        nameField.tap()
        nameField.typeText("Editable Rack")

        let saveButton = app.buttons["saveRackButton"]
        XCTAssertTrue(saveButton.exists)
        saveButton.tap()

        let rackCell = app.staticTexts["Editable Rack"]
        XCTAssertTrue(rackCell.waitForExistence(timeout: 5))
        rackCell.tap()

        let menuButton = app.buttons["rackDetailMenuButton"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 5))
        menuButton.tap()

        let editButton = app.buttons["Edit"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 5))
        editButton.tap()

        let editSaveButton = app.buttons["editRackSaveButton"]
        let editCancelButton = app.buttons["editRackCancelButton"]
        XCTAssertTrue(editSaveButton.waitForExistence(timeout: 5))
        XCTAssertTrue(editCancelButton.exists)

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
}
