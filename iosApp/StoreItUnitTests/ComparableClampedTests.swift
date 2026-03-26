import XCTest
@testable import StoreIt

final class ComparableClampedTests: XCTestCase {

    func testClamped_whenValueWithinRange_returnsSameValue() {
        let value = 5
        let result = value.clamped(to: 0...10)

        XCTAssertEqual(result, 5)
    }

    func testClamped_whenValueBelowRange_returnsLowerBound() {
        let value = -3
        let result = value.clamped(to: 0...10)

        XCTAssertEqual(result, 0)
    }

    func testClamped_whenValueAboveRange_returnsUpperBound() {
        let value = 42
        let result = value.clamped(to: 0...10)

        XCTAssertEqual(result, 10)
    }
}

